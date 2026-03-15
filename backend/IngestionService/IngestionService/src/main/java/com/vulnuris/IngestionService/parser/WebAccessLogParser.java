package com.vulnuris.IngestionService.parser;


import com.vulnuris.IngestionService.model.ces.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.*;
import java.util.stream.Stream;

@Slf4j
@Component
public class WebAccessLogParser implements LogParser {

    // Apache / Nginx combined log pattern
    private static final Pattern LOG_PATTERN = Pattern.compile(
            "^(\\S+) - - \\[(.*?)\\] \"(\\S+) (.*?) (\\S+)\" (\\d{3}) (\\d+) \"(.*?)\" \"(.*?)\"$"
    );

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);

    @Override
    public Stream<CesEvent> parseStream(InputStream input, String filename) {

        AtomicLong offsetCounter = new AtomicLong(0);

        BufferedReader reader = new BufferedReader(new InputStreamReader(input));

        return reader.lines()
                .map(line -> parseLine(line, filename, offsetCounter.getAndIncrement()))
                .filter(Objects::nonNull);
    }

    private CesEvent parseLine(String line, String file, long offset) {

        try {

            if (line == null || line.isBlank())
                return null;

            Matcher m = LOG_PATTERN.matcher(line);

            if (!m.matches())
                return null;

            String ip = safe(m.group(1));
            String timestamp = safe(m.group(2));
            String method = safe(m.group(3));
            String url = safe(m.group(4));
            String protocol = safe(m.group(5));
            Integer status = safeInt(m.group(6));
            String userAgent = safe(m.group(9));

            Instant tsUtc = parseTime(timestamp);

            String action = detectAction(method, url);
            ResultType result = mapResult(status);

            List<String> ips = ip != null ? List.of(ip) : List.of();

            return CesEvent.builder()

                    .tsUtc(tsUtc)
                    .tsOriginal(timestamp)
                    .tzOffset("+00:00")

                    .sourceType(SourceType.WEB)

                    .srcIp(ip)

                    .protocol(protocol)

                    .action(action)
                    .object(url)

                    .result(result)

                    .message(userAgent)

                    .iocs(Iocs.builder()
                            .ips(ips)
                            .urls(url != null ? List.of(url) : List.of())
                            .build())

                    .correlationKeys(CorrelationKeys.builder()
                            .srcIp(ip)
                            .object(url)
                            .build())

                    .rawRef(RawRef.builder()
                            .file(file)
                            .offset(offset)
                            .build())

                    .build();

        } catch (Exception e) {

            log.warn("Web log parsing failed: {}", e.getMessage());
            return null;
        }
    }

    private Instant parseTime(String time) {

        try {
            ZonedDateTime zdt = ZonedDateTime.parse(time, FORMATTER);
            return zdt.toInstant();
        } catch (Exception e) {
            return Instant.now();
        }
    }

    private ResultType mapResult(Integer status) {

        if (status == null)
            return ResultType.UNKNOWN;

        if (status >= 200 && status < 300)
            return ResultType.SUCCESS;

        if (status >= 400)
            return ResultType.FAIL;

        return ResultType.UNKNOWN;
    }

    private String detectAction(String method, String url) {

        if (url == null)
            return method;

        String lower = url.toLowerCase();

        if (lower.contains("<script>"))
            return "XSS_ATTEMPT";

        if (lower.contains("../"))
            return "PATH_TRAVERSAL";

        if (lower.contains("' or '1'='1"))
            return "SQL_INJECTION";

        if (lower.contains("cmd="))
            return "COMMAND_INJECTION";

        if (lower.contains("wp-admin"))
            return "WORDPRESS_PROBE";

        return method;
    }

    private String safe(String val) {
        return val != null ? val : null;
    }

    private Integer safeInt(String val) {

        if (val == null)
            return null;

        try {
            return Integer.parseInt(val);
        } catch (Exception e) {
            return null;
        }
    }
}
