package com.caophuc.court.service.impl;

import com.caophuc.court.service.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalStorageServiceImpl implements StorageService {

    private final Path rootLocation = Paths.get("uploads");

    public LocalStorageServiceImpl() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage location", e);
        }
    }

    @Override
    public String store(MultipartFile file, String subfolder) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file.");
            }

            Path destinationFolder = rootLocation.resolve(subfolder).normalize().toAbsolutePath();

            Files.createDirectories(destinationFolder);
            String uniqueFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

            try (InputStream inputStream = file.getInputStream()) {
                Path destinationFile = destinationFolder.resolve(uniqueFileName);
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            return "/" + rootLocation.getFileName().toString() + "/" + subfolder + "/" + uniqueFileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file.", e);
        }
    }

    @Override
    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }
        try {
            String filePath = fileUrl.startsWith("/") ? fileUrl.substring(1) : fileUrl;
            
            // Xóa tiền tố "uploads/" khỏi fileUrl vì rootLocation đã là "uploads"
            if (filePath.startsWith(rootLocation.getFileName().toString() + "/")) {
                filePath = filePath.substring(rootLocation.getFileName().toString().length() + 1);
            }
            
            Path file = rootLocation.resolve(filePath).normalize();
            Files.deleteIfExists(file);
        } catch (IOException e) {
            System.err.println("Failed to delete file: " + fileUrl);
        }
    }
}
