package com.vulnuris.IngestionService.parser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vulnuris.IngestionService.model.ces.*;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@Component
public class CloudTrailParser implements LogParser {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Stream<CesEvent> parseStream(InputStream input, String filename) {

        try {
            Map<String, Object> root = mapper.readValue(input, Map.class);

            List<Map<String, Object>> records = new ArrayList<>();

            // ✅ Handle AWS CloudTrail wrapped format
            if (root.containsKey("Records")) {
                Object recObj = root.get("Records");

                if (recObj instanceof List<?>) {
                    records = (List<Map<String, Object>>) recObj;
                }
            } else {
                // ✅ Handle single log or direct JSON
                records.add(root);
            }

            return records.stream()
                    .filter(Objects::nonNull)
                    .map(record -> safeConvert(record, filename))
                    .filter(Objects::nonNull);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("CloudTrail parsing error", e);
        }
    }

    private CesEvent safeConvert(Map<String, Object> log, String file) {

        try {
            if (log == null || log.isEmpty()) {
                return null;
            }

            // ✅ Safe eventTime extraction
            String eventTime = getString(log, "eventTime");
            Instant tsUtc = null;

            if (eventTime != null) {
                tsUtc = Instant.parse(eventTime);
            } else {
                tsUtc = Instant.now(); // fallback
            }

            // ✅ Safe nested userIdentity
            Map<String, Object> userIdentity =
                    getMap(log, "userIdentity");

            String user = null;
            if (userIdentity != null) {
                user = getString(userIdentity, "userName");
            }

            // ✅ Safe IP extraction
            String srcIp = getString(log, "sourceIPAddress");

            // ✅ Safe action and object
            String action = getString(log, "eventName");
            String object = getString(log, "eventSource");

            // ✅ Safe IOC list
            List<String> ips = new ArrayList<>();
            if (srcIp != null && !srcIp.isBlank()) {
                ips.add(srcIp);
            }

            return CesEvent.builder()
                    .tsUtc(tsUtc)
                    .tsOriginal(eventTime)
                    .tzOffset("Z")

                    .sourceType(SourceType.CLOUD)

                    .user(user)
                    .srcIp(srcIp)

                    .action(action)
                    .object(object)

                    .result(ResultType.UNKNOWN)

                    .iocs(Iocs.builder()
                            .ips(ips)
                            .build())

                    .correlationKeys(CorrelationKeys.builder()
                            .user(user)
                            .srcIp(srcIp)
                            .build())

                    .rawRef(RawRef.builder()
                            .file(file)
                            .offset(0L)
                            .build())

                    .message("AWS CloudTrail event")
                    .build();

        } catch (Exception e) {
            System.err.println("Failed to parse CloudTrail log: " + log);
            e.printStackTrace();
            return null; // skip bad logs instead of breaking pipeline
        }
    }

    // 🔥 Utility methods for null safety

    private String getString(Map<String, Object> map, String key) {
        if (map == null || key == null) return null;
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private Map<String, Object> getMap(Map<String, Object> map, String key) {
        if (map == null || key == null) return null;
        Object val = map.get(key);
        if (val instanceof Map<?, ?>) {
            return (Map<String, Object>) val;
        }
        return null;
    }
}
