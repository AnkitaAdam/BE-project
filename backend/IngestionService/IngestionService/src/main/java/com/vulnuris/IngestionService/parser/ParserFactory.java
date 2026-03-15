package com.vulnuris.IngestionService.parser;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParserFactory {

    private final CloudTrailParser cloudTrail;
    private final O365Parser o365;
    private final PaloAltoFirewallParser paloAlto;
    private final SyslogParser syslog;

    public LogParser getParser(String source) {
        return switch (source.toLowerCase()) {
            case "aws" -> cloudTrail;
            case "o365" -> o365;
            case "paloalto" -> paloAlto;
            case "os" -> syslog;
            default -> throw new RuntimeException("Unsupported source");
        };
    }
}
