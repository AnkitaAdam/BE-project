package com.vulnuris.IngestionService.parser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vulnuris.IngestionService.model.ces.*;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Component
public class PaloAltoFirewallParser implements LogParser {

    private final ObjectMapper mapper = new ObjectMapper();

    private static final DateTimeFormatter PA_TIME =
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
                    .withZone(ZoneOffset.UTC);

    @Override
    public Stream<CesEvent> parseStream(InputStream input, String filename) {

        try {

            AtomicLong offsetCounter = new AtomicLong(0);

            List<Map<String, Object>> logs =
                    mapper.readValue(input, new TypeReference<>() {});

            if (logs == null || logs.isEmpty()) {
                return Stream.empty();
            }

            return logs.stream()
                    .map(log -> convert(log, filename, offsetCounter.getAndIncrement()))
                    .filter(Objects::nonNull);

        } catch (Exception e) {
            throw new RuntimeException("Palo Alto parsing error", e);
        }
    }

    private CesEvent convert(Map<String, Object> log, String file, long offset) {

        if (log == null) return null;

        try {

            String time = safeString(log.get("generated_time"));
            Instant tsUtc = parseTime(time);

            String srcIp = safeString(log.get("src"));
            String dstIp = safeString(log.get("dst"));
            String user = extractUser(safeString(log.get("src_user")));

            String action = safeString(log.get("action"));
            String protocol = safeString(log.get("protocol"));

            Integer srcPort = safeInt(log.get("sport"));
            Integer dstPort = safeInt(log.get("dport"));

            String threat = safeString(log.get("threat_name"));
            String category = safeString(log.get("category"));

            int severity = mapSeverity(safeString(log.get("severity")));

            return CesEvent.builder()

                    .tsUtc(tsUtc)
                    .tsOriginal(time)
                    .tzOffset("+00:00")

                    .sourceType(SourceType.FIREWALL)

                    .srcIp(srcIp)
                    .dstIp(dstIp)
                    .srcPort(srcPort)
                    .dstPort(dstPort)

                    .user(user)

                    .protocol(protocol)
                    .action(action)

                    .object(category)

                    .result(mapResult(action))

                    .severity(severity)

                    .message(buildMessage(action, threat, category))

                    .iocs(Iocs.builder()
                            .ips(buildIpList(srcIp, dstIp))
                            .build())

                    .correlationKeys(CorrelationKeys.builder()
                            .user(user)
                            .srcIp(srcIp)
                            .dstIp(dstIp)
                            .build())

                    .rawRef(RawRef.builder()
                            .file(file)
                            .offset(offset)
                            .build())

                    .build();

        } catch (Exception e) {
            return null; // prevent crash
        }
    }

    // ---------- Helper methods ----------

    private String safeString(Object o) {
        return o == null ? null : o.toString();
    }

    private Integer safeInt(Object o) {
        try {
            return o == null ? null : Integer.parseInt(o.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Instant parseTime(String time) {
        try {
            if (time == null) return Instant.now();
            return Instant.from(PA_TIME.parse(time));
        } catch (Exception e) {
            return Instant.now();
        }
    }

    private String extractUser(String user) {
        if (user == null) return null;
        if (user.contains("\\")) {
            return user.split("\\\\")[1];
        }
        return user;
    }

    private int mapSeverity(String severity) {
        if (severity == null) return 0;

        return switch (severity.toLowerCase()) {
            case "critical" -> 9;
            case "high" -> 7;
            case "medium" -> 5;
            case "low" -> 3;
            default -> 1;
        };
    }

    private ResultType mapResult(String action) {
        if (action == null) return ResultType.UNKNOWN;

        if (action.equalsIgnoreCase("allow"))
            return ResultType.SUCCESS;

        return ResultType.FAIL;
    }

    private List<String> buildIpList(String src, String dst) {
        List<String> ips = new ArrayList<>();
        if (src != null) ips.add(src);
        if (dst != null) ips.add(dst);
        return ips;
    }

    private String buildMessage(String action, String threat, String category) {
        return "Firewall " + action +
                (threat != null ? " | threat=" + threat : "") +
                (category != null ? " | category=" + category : "");
    }
}
