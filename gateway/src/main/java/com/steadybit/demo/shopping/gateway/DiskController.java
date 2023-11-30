/*
 * Copyright 2019 steadybit GmbH. All rights reserved.
 */

package com.steadybit.demo.shopping.gateway;

import io.netty.buffer.ByteBufUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.info.OsInfo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
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
    public ResponseEntity getFillDisk(@PathVariable("size") String size) {
        log.info("Filling disk with {}", size);

        // Convert the size to bytes
        long bytes = toBytes(size);
        if (bytes < 0) {
            throw new RuntimeException("Invalid size: " + size);
        }

        // create a temporary file
        Path tempFile = null;
        var deleted = true;
        try {
            if(!Files.exists(Path.of("/work"))) {
                tempFile = Files.createTempFile("tempFile", ".tmp");
            }else {
                tempFile = Files.createFile(Path.of("/work", "tempFile.tmp"));
            }
            log.info("Created temporary file {}", tempFile);
            BufferedOutputStream fo = new BufferedOutputStream(new FileOutputStream(tempFile.toFile()));

            // 4MB Byte Array
            var defaultByteArrayLength = 4 * 1024 * 1024;
            byte[] byteArray = new byte[defaultByteArrayLength];
            long bytesWritten = 0;
            while (bytesWritten < bytes) {
                log.info("Writing {} bytes to temporary file", byteArray.length);
                if (bytesWritten + byteArray.length > bytes) {
                    var lastBytesToWrite = bytes - bytesWritten;
                    fo.write(byteArray, 0, (int) lastBytesToWrite);
                    bytesWritten += lastBytesToWrite;
                } else {
                    fo.write(byteArray);
                    bytesWritten += byteArray.length;
                }
            }
            fo.flush();
            fo.close();
        } catch (IOException e) {
            log.error("Failed to create temporary file", e);
            return new ResponseEntity(HttpStatus.INSUFFICIENT_STORAGE);
        } finally {
            log.info("Deleting temporary file");
            if (tempFile != null) {
                deleted = tempFile.toFile().delete();
            }
        }

        if (!deleted) {
            log.warn("Failed to delete temporary file");
            throw new RuntimeException("Failed to delete temporary file");
        }
        log.info("Created temporary file");
        // Writes a string to the above temporary file
        return new ResponseEntity("Filled disk with %d bytes and deleted afterwards".formatted(bytes), HttpStatus.OK);
    }
    @GetMapping("/size")
    public ResponseEntity getExpectedDiskSize(@RequestParam("expectedSize") String expectedSize, @RequestParam("path") String path) {
        log.info("Checking disk size for {}", path);
        var expectedBytes = toBytes(expectedSize);
        if (expectedBytes < 0) {
            throw new RuntimeException("Invalid size: " + expectedSize);
        }
        File file = new File(path);
        var actualBytes = file.getFreeSpace();
        log.info("Disk size is {} bytes", actualBytes);
        if (actualBytes < expectedBytes) {
            var errorMessage = "Expected disk size %d bytes but got %d bytes".formatted(expectedBytes, actualBytes);
            log.error(errorMessage);
            return new ResponseEntity(errorMessage, HttpStatus.INSUFFICIENT_STORAGE);
        }
        log.info("Disk size is ok");
        return new ResponseEntity("Ok. "+toHumanReadableByNumOfLeadingZeros(actualBytes), HttpStatus.OK);
    }

    public static String toHumanReadableByNumOfLeadingZeros(long size) {
        if (size < 0) {
            throw new IllegalArgumentException("Invalid file size: " + size);
        }
        if (size < 1024) return size + " Bytes";
        int unitIdx = (63 - Long.numberOfLeadingZeros(size)) / 10;
        return formatSize(size, 1L << (unitIdx * 10), " KMGTPE".charAt(unitIdx) + "iB");
    }

    private static String formatSize(long size, long divider, String unitName) {
        return DEC_FORMAT.format((double) size / divider) + " " + unitName;
    }

    private static DecimalFormat DEC_FORMAT = new DecimalFormat("#.##");



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


