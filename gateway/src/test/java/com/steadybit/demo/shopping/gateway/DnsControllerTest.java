/*
 * Copyright 2026 steadybit GmbH. All rights reserved.
 */

package com.steadybit.demo.shopping.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DnsController.class)
class DnsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldResolveValidHostname() throws Exception {
        mockMvc.perform(post("/api/dns/resolve/localhost"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostname").value("localhost"))
                .andExpect(jsonPath("$.addresses").isNotEmpty())
                .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    void shouldReturnErrorForUnknownHostname() throws Exception {
        mockMvc.perform(post("/api/dns/resolve/this.host.does.not.exist.invalid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostname").value("this.host.does.not.exist.invalid"))
                .andExpect(jsonPath("$.addresses").isEmpty())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }
}
