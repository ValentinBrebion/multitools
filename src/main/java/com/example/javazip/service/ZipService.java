package com.example.javazip.service;

import java.io.*;
import java.nio.file.*;
import java.util.zip.*;
import java.util.*;
import java.util.stream.Stream;

public class ZipService {
    
    public static void compressFiles(List<Path> sourcePaths, Path zipPath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipPath.toFile());
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            
            for (Path sourcePath : sourcePaths) {
                if (Files.isDirectory(sourcePath)) {
                    compressDirectory(sourcePath, sourcePath.getFileName().toString(), zos);
                } else {
                    compressFile(sourcePath, zos);
                }
            }
        }
    }
    
    private static void compressFile(Path filePath, ZipOutputStream zos) throws IOException {
        ZipEntry zipEntry = new ZipEntry(filePath.getFileName().toString());
        zos.putNextEntry(zipEntry);
        
        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
        }
        
        zos.closeEntry();
    }
    
    private static void compressDirectory(Path dirPath, String basePath, ZipOutputStream zos) throws IOException {
        File[] files = dirPath.toFile().listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    compressDirectory(file.toPath(), basePath + "/" + file.getName(), zos);
                } else {
                    ZipEntry zipEntry = new ZipEntry(basePath + "/" + file.getName());
                    zos.putNextEntry(zipEntry);
                    
                    try (FileInputStream fis = new FileInputStream(file)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                    }
                    
                    zos.closeEntry();
                }
            }
        }
    }
    
    public static long calculateCompressionRatio(List<Path> sourcePaths, Path zipPath) throws IOException {
        long originalSize = 0;
        for (Path path : sourcePaths) {
            if (Files.isDirectory(path)) {
                originalSize += getDirectorySize(path);
            } else {
                originalSize += Files.size(path);
            }
        }
        
        long compressedSize = Files.size(zipPath);
        
        if (originalSize == 0) return 0;
        return ((originalSize - compressedSize) * 100) / originalSize;
    }
    
    private static long getDirectorySize(Path dirPath) throws IOException {
        long size = 0;
        try (Stream<Path> stream = Files.walk(dirPath)) {
            size = stream
                .filter(p -> p.toFile().isFile())
                .mapToLong(p -> {
                    try {
                        return Files.size(p);
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .sum();
        }
        return size;
    }
}

