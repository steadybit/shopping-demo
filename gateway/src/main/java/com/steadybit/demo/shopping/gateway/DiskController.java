/*
 * Copyright 2019 steadybit GmbH. All rights reserved.
 */

package com.steadybit.demo.shopping.gateway;

import io.netty.buffer.ByteBufUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/disk")
public class DiskController {
    private static final Logger log = LoggerFactory.getLogger(GatewayApplication.class);

    public DiskController() {
    }

    @GetMapping("/fill/{size}")
    public String getFillDisk(@PathVariable("size") String size) {
        log.debug("Filling disk with {}", size);

        // Convert the size to bytes
        long bytes = toBytes(size);
        if (bytes < 0) {
            throw new RuntimeException("Invalid size: " + size);
        }

        // create a temporary file
        Path tempFile = null;
        var deleted = true;
        try {
            tempFile = Files.createTempFile("diskFill", ".tmp");
            FileOutputStream fo = new FileOutputStream(tempFile.toFile());
            // write bytes to the file
            int byteArraySize = 256 * 1024 * 1024;
            var writeCount = bytes / byteArraySize;
            for (long i = 0; i < writeCount; i++) {
                fo.write(new byte[byteArraySize]);
            }
            var restByte = bytes % byteArraySize;
            fo.write(new byte[(int) restByte]);
            fo.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            log.debug("Deleting temporary file");
            if (tempFile != null) {
                deleted = tempFile.toFile().delete();
            }
        }

        if (!deleted) {
            log.warn("Failed to delete temporary file");
            throw new RuntimeException("Failed to delete temporary file");
        }
        log.debug("Created temporary file");
        // Writes a string to the above temporary file
        return "Filled disk with %d bytes and deleted afterwards".formatted(bytes);
    }

    public static long toBytes(String filesize) {
        long returnValue = -1;
        Pattern patt = Pattern.compile("([\\d.]+)([GMK]B)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = patt.matcher(filesize);
        Map<String, Integer> powerMap = new HashMap<>();
        powerMap.put("GB", 3);
        powerMap.put("MB", 2);
        powerMap.put("KB", 1);
        if (matcher.find()) {
            String number = matcher.group(1);
            int pow = powerMap.get(matcher.group(2).toUpperCase());
            BigDecimal bytes = new BigDecimal(number);
            bytes = bytes.multiply(BigDecimal.valueOf(1024).pow(pow));
            returnValue = bytes.longValue();
        }
        return returnValue;
    }
}


