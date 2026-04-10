/*
 * Copyright 2026 steadybit GmbH. All rights reserved.
 */

package com.steadybit.demo.shopping.gateway;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.annotation.PreDestroy;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.resolver.dns.DnsNameResolver;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import io.netty.resolver.dns.NoopDnsCache;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.util.List;

@RestController
@RequestMapping("/api/dns")
public class DnsController {

    private final DnsNameResolver resolver;

    public DnsController() {
        this.resolver = new DnsNameResolverBuilder(new NioEventLoopGroup(1).next())
                .channelType(NioDatagramChannel.class)
                .resolveCache(NoopDnsCache.INSTANCE)
                .queryTimeoutMillis(5000)
                .build();
    }

    @PreDestroy
    void close() {
        resolver.close();
    }

    @PostMapping("/resolve/{hostname}")
    @RateLimiter(name = "dnsResolve")
    public DnsResult resolve(@PathVariable String hostname) {
        try {
            List<String> addresses = resolver.resolveAll(hostname).get().stream()
                    .map(InetAddress::getHostAddress)
                    .toList();
            return new DnsResult(hostname, addresses, null);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return new DnsResult(hostname, List.of(), cause.getMessage());
        }
    }

    public record DnsResult(String hostname, List<String> addresses, String error) {}
}
