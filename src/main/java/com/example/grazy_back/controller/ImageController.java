package com.example.grazy_back.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.grazy_back.model.ImageMetadata;
import com.example.grazy_back.service.ImageStorageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/api/images")
@Tag(name = "Imagens", description = "Upload, listagem, download e exclusão de imagens")
public class ImageController
{

    private final ImageStorageService storage;

    public ImageController(ImageStorageService storage)
    {
        this.storage = storage;
    }

    @PostMapping("/upload")
    @Operation(summary = "Upload de imagem",
        description = "Envia uma imagem para o servidor")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "forServico", required = false, defaultValue = "false") boolean forServico) throws IOException 
    {
        ImageMetadata saved = storage.storeMultipart(file, forServico);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/from-url")
    @Operation(summary = "Upload por URL",
        description = "Baixa imagem de uma URL e salva no servidor")
    public ResponseEntity<?> fromUrl(@RequestBody Map<String, Object> body) throws Exception
    {
        String url = (String) body.get("url");
        boolean forServico = false;

        if (body.containsKey("forServico")) 
        {
            Object v = body.get("forServico");
            if (v instanceof Boolean) forServico = (Boolean) v;
            else if (v instanceof String) forServico = Boolean.parseBoolean((String) v);
        }

        ImageMetadata saved = storage.storeFromUrl(url, forServico);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    @Operation(summary = "Lista imagens")
    public ResponseEntity<?> listImages() 
    {
        return ResponseEntity.ok(storage.listAllImages());
    }

    @GetMapping("/landing")
    @Operation(summary = "Lista imagens para landing page (ordenadas)")
    public ResponseEntity<?> listLandingImages() 
    {
        return ResponseEntity.ok(storage.listLandingImages());
    }

    @PutMapping("/{storedFilename}")
    @Operation(summary = "Atualiza metadados da imagem")
    public ResponseEntity<?> updateMetadata(@PathVariable String storedFilename, @RequestBody Map<String, Object> body) 
    {
        ImageMetadata updated = storage.updateMetadata(storedFilename, body);
        if (updated == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/ordenar")
    @Operation(summary = "Atualiza ordem das imagens na landing")
    public ResponseEntity<?> updateOrder(@RequestBody List<String> storedFilenames) 
    {
        storage.updateLandingOrder(storedFilenames);
        return ResponseEntity.ok(Map.of("success", true, "message", "Ordem atualizada com sucesso"));
    }

    @GetMapping("/download/{storedFilename}")
    @Operation(summary = "Download de imagem")
    public ResponseEntity<byte[]> download(@PathVariable String storedFilename) throws IOException
    {
        Path path = storage.loadAsPath(storedFilename);

        if (!Files.exists(path)) 
            return ResponseEntity.notFound().build();

        byte[] content = Files.readAllBytes(path);
        String contentType = Files.probeContentType(path);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + storedFilename + "\"")
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .body(content);
    }

    @DeleteMapping("/{storedFilename}")
    @Operation(summary = "Exclui imagem")
    public ResponseEntity<?> delete(@PathVariable String storedFilename) throws IOException 
    {
        boolean deleted = storage.deleteByStoredFilename(storedFilename);

        if (!deleted)
            return ResponseEntity.notFound().build();

        return ResponseEntity.noContent().build();
    }
}
