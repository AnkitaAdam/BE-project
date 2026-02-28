package com.vulnuris.IngestionService.service;

import com.vulnuris.IngestionService.kafka.KafkaProducerService;
import com.vulnuris.IngestionService.parser.LogParser;
import com.vulnuris.IngestionService.parser.ParserFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class IngestionService {

    private final ParserFactory parserFactory;
    private final KafkaProducerService kafkaProducer;

    @Async
    public void processFile(MultipartFile file, String sourceType) {

        LogParser parser = parserFactory.getParser(sourceType);

        try (InputStream is = file.getInputStream()) {

            parser.parseStream(is, file.getOriginalFilename())
                    .forEach(kafkaProducer::send);

        } catch (Exception e) {
            throw new RuntimeException("Error processing log", e);
        }
    }
}
