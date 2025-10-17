package com.example.grazy_back.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.grazy_back.model.ImageMetadata;
import com.example.grazy_back.repository.ImageMetadataRepository;

@Service
public class ImageStorageService
{

    private final Path storageRoot;
    private final ImageMetadataRepository repo;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public ImageStorageService(@Value("${file.storage.location:uploads}") String storageLocation,
            ImageMetadataRepository repo)
    {

        Path candidate = Paths.get(storageLocation);

        if (!candidate.isAbsolute())
            candidate = Paths.get(System.getProperty("user.home"), storageLocation);

        this.storageRoot = candidate.toAbsolutePath().normalize();
        this.repo = repo;

        try 
        {
            Files.createDirectories(this.storageRoot);
        }
        catch (IOException e) 
        {
            throw new IllegalStateException("Unable to create storage directory: " + this.storageRoot, e);
        }
    }

    public ImageMetadata storeMultipart(MultipartFile file, boolean forServico) throws IOException
    {
        validateImage(file.getContentType());

        String ext = getExtension(file.getOriginalFilename(), file.getContentType());
        String stored = UUID.randomUUID().toString() + ext;
        Path target = storageRoot.resolve(stored);

        try (InputStream in = file.getInputStream())
        {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

    ImageMetadata meta = new ImageMetadata();
        meta.setOriginalFilename(file.getOriginalFilename());
        meta.setStoredFilename(stored);
        meta.setContentType(file.getContentType());
        meta.setSize(file.getSize());
    meta.setForServico(Boolean.valueOf(forServico));
        meta.setCreatedAt(Instant.now());
        return repo.save(meta);
    }

    public ImageMetadata storeFromUrl(String urlString, boolean forServico) throws IOException, InterruptedException 
    {
        URI uri = URI.create(urlString);
        
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) 
            throw new IllegalArgumentException("URL inválida");

        HttpRequest req = HttpRequest.newBuilder(uri)
                .GET()
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "grazy-back/1.0")
                .build();

        HttpResponse<InputStream> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream());

        if (resp.statusCode() != 200)
            throw new IOException("Erro ao baixar imagem: " + resp.statusCode());

        String contentType = resp.headers().firstValue("Content-Type").orElse("application/octet-stream");
        validateImage(contentType);

        String ext = getExtension(uri.getPath(), contentType);
        String stored = UUID.randomUUID().toString() + ext;
        Path target = storageRoot.resolve(stored);

        try (InputStream in = resp.body()) 
        {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

    ImageMetadata meta = new ImageMetadata();
        meta.setOriginalFilename(Paths.get(uri.getPath()).getFileName().toString());
        meta.setStoredFilename(stored);
        meta.setContentType(contentType);
        meta.setSize(Files.size(target));
        meta.setSourceUrl(urlString);
    meta.setForServico(Boolean.valueOf(forServico));
        meta.setCreatedAt(Instant.now());
        return repo.save(meta);
    }

    private void validateImage(String contentType)
    {
        if (contentType == null)
            throw new IllegalArgumentException("Content-Type ausente");
        if (!contentType.startsWith("image/"))
            throw new IllegalArgumentException("Tipo não suportado: " + contentType);
    }

    private String getExtension(String filenameOrPath, String contentType) 
    {
        String ext = "";
        if (filenameOrPath != null && filenameOrPath.contains("."))
        {
            ext = filenameOrPath.substring(filenameOrPath.lastIndexOf('.'));
            if (ext.length() <= 1)
                ext = "";
        }

        if (ext.isBlank()) 
        {
            if ("image/png".equals(contentType))
                return ".png";
            if ("image/jpeg".equals(contentType) || "image/jpg".equals(contentType))
                return ".jpg";
            if ("image/gif".equals(contentType))
                return ".gif";
            return ".bin";
        }

        return ext;
    }

    public Path loadAsPath(String storedFilename) 
    {
        return storageRoot.resolve(storedFilename).normalize();
    }

    public List<String> listAllStoredFilenames() 
    {
        return repo.findAllByForServicoFalse().stream()
            .map(img -> img.getStoredFilename())
            .collect(Collectors.toList());
    }

    public boolean deleteByStoredFilename(String storedFilename) throws IOException 
    {
        Optional<ImageMetadata> opt = repo.findByStoredFilename(storedFilename);

        if (opt.isEmpty()) 
            return false;
        
        ImageMetadata meta = opt.get();
        Path path = loadAsPath(meta.getStoredFilename());
        Files.deleteIfExists(path);
        repo.delete(meta);
        return true;
    }
}
