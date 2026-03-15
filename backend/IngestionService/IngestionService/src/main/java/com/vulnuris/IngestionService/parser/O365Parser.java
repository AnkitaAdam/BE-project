package com.vulnuris.IngestionService.parser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vulnuris.IngestionService.model.ces.*;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Component
public class O365Parser implements LogParser {

    private final ObjectMapper mapper = new ObjectMapper();

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
            throw new RuntimeException("O365 parsing error", e);
        }
    }

    private CesEvent convert(Map<String, Object> log, String file, long offset) {

        if (log == null) return null;

        try {

            // ---------- Timestamp ----------
            String creationTime = safeString(log.get("CreationTime"));
            Instant tsUtc = parseTime(creationTime);

            // ---------- User ----------
            String user = safeString(log.get("UserId"));

            // ---------- IP ----------
            String srcIp = safeString(log.get("ClientIP"));

            // ---------- Action ----------
            String action = safeString(log.get("Operation"));

            // ---------- Object ----------
            String workload = safeString(log.get("Workload"));

            // ---------- Result ----------
            String resultStatus = safeString(log.get("ResultStatus"));
            ResultType result = mapResult(resultStatus);

            // ---------- IOC ----------
            List<String> ips = new ArrayList<>();
            if (srcIp != null) ips.add(srcIp);

            return CesEvent.builder()
                    .tsUtc(tsUtc)
                    .tsOriginal(creationTime)
                    .tzOffset("+00:00")

                    .sourceType(SourceType.IDP)

                    .user(user)
                    .srcIp(srcIp)

                    .action(action)
                    .object(workload)

                    .result(result)

                    .message(buildMessage(user, action, srcIp))

                    .iocs(Iocs.builder()
                            .ips(ips)
                            .build())

                    .correlationKeys(CorrelationKeys.builder()
                            .user(user)
                            .srcIp(srcIp)
                            .build())

                    .rawRef(RawRef.builder()
                            .file(file)
                            .offset(offset)
                            .build())

                    .build();

        } catch (Exception e) {
            return null;
        }
    }

    // -------- Helper methods --------

    private String safeString(Object o) {
        return o == null ? null : o.toString();
    }

    private Instant parseTime(String time) {
        try {
            if (time == null) return Instant.now();

            // O365 timestamp without timezone
            LocalDateTime ldt = LocalDateTime.parse(time);
            return ldt.toInstant(ZoneOffset.UTC);

        } catch (Exception e) {
            return Instant.now();
        }
    }

    private ResultType mapResult(String status) {
        if (status == null) return ResultType.UNKNOWN;

        if (status.equalsIgnoreCase("Success"))
            return ResultType.SUCCESS;

        if (status.equalsIgnoreCase("Failed"))
            return ResultType.FAIL;

        return ResultType.UNKNOWN;
    }

    private String buildMessage(String user, String action, String ip) {
        return "O365 event: " + action +
                (user != null ? " by " + user : "") +
                (ip != null ? " from " + ip : "");
    }
}