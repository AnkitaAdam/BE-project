package com.vulnuris.IngestionService.parser;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParserFactory {

    private final CloudTrailParser cloudTrail;
    private final O365Parser o365;
    private final PaloAltoFirewallParser paloAlto;

    public LogParser getParser(String source) {
        return switch (source.toLowerCase()) {
            case "aws" -> cloudTrail;
            case "o365" -> o365;
            case "paloalto" -> paloAlto;
            default -> throw new RuntimeException("Unsupported source");
        };
    }
}
