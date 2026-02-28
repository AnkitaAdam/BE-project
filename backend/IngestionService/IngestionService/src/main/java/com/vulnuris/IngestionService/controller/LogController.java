package com.vulnuris.IngestionService.controller;

import com.vulnuris.IngestionService.service.IngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
public class LogController {

    private final IngestionService ingestionService;

    @PostMapping("/upload")
    public ResponseEntity<String> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sourceType") String sourceType) {

        ingestionService.processFile(file, sourceType);
        return ResponseEntity.ok("File uploaded and processing started");
    }
}
