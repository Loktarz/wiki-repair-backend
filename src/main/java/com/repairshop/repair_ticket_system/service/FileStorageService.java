package com.repairshop.repair_ticket_system.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Saves uploaded files (e.g. signed Bon de Réception PDFs) to a configurable
 * directory on disk. The directory is created on startup if it doesn't exist.
 */
@Service
@Slf4j
public class FileStorageService {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDirRaw;

    private Path uploadDir;

    @PostConstruct
    public void init() throws IOException {
        uploadDir = Paths.get(uploadDirRaw).toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);
        Files.createDirectories(uploadDir.resolve("signed-bons"));
        log.info("File storage initialized at: {}", uploadDir);
    }

    /**
     * Stores a signed Bon de Réception PDF and returns the relative path.
     */
    public String storeSignedBon(MultipartFile file, String ticketNumber) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Fichier vide");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.contains("pdf")) {
            throw new RuntimeException("Le fichier doit être un PDF");
        }
        try {
            String safeTicket = ticketNumber == null ? "ticket" : ticketNumber.replaceAll("[^a-zA-Z0-9_-]", "_");
            String filename = safeTicket + "_signed_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
            Path target = uploadDir.resolve("signed-bons").resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Signed bon stored at: {}", target);
            return "signed-bons/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Erreur stockage fichier: " + e.getMessage());
        }
    }

    /**
     * Reads a stored file's bytes from disk. Used to serve uploads back to the client.
     */
    public byte[] read(String relativePath) {
        try {
            Path target = uploadDir.resolve(relativePath).normalize();
            // Security: ensure the path stays inside uploadDir
            if (!target.startsWith(uploadDir)) {
                throw new RuntimeException("Chemin invalide");
            }
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lecture fichier: " + e.getMessage());
        }
    }
}
