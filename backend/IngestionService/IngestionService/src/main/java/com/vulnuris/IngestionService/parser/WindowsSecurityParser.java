package com.vulnuris.IngestionService.parser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vulnuris.IngestionService.model.ces.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Slf4j
@Component
public class WindowsSecurityParser implements LogParser {

    private final ObjectMapper mapper = new ObjectMapper();

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Stream<CesEvent> parseStream(InputStream input, String filename) {

        try {

            List<Map<String, Object>> events =
                    mapper.readValue(input, new TypeReference<List<Map<String, Object>>>() {});

            // offset counter
            AtomicLong offsetCounter = new AtomicLong(0);

            return events.stream()
                    .map(event -> convert(event, filename, offsetCounter.getAndIncrement()))
                    .filter(Objects::nonNull);

        } catch (Exception e) {
            log.error("Windows parser failed", e);
            return Stream.empty();
        }
    }

    private CesEvent convert(Map<String, Object> log, String file,long offset) {

        try {

            if (log == null) return null;

            String eventTime = safeString(log.get("EventTime"));
            String hostname = safeString(log.get("Hostname"));
            String message = safeString(log.get("Message"));

            Integer eventId = safeInt(log.get("EventID"));

            Map<String, Object> eventData =
                    (Map<String, Object>) log.getOrDefault("EventData", new HashMap<>());

            String subjectUser = safeString(eventData.get("SubjectUserName"));
            String targetUser = safeString(eventData.get("TargetUserName"));
            String ipAddress = safeString(eventData.get("IpAddress"));
            Integer ipPort = safeInt(eventData.get("IpPort"));

            String user = targetUser != null ? targetUser : subjectUser;

            Instant tsUtc = parseTime(eventTime);

            String action = mapAction(eventId);
            ResultType result = mapResult(log.get("EventType"));

            List<String> ips = ipAddress != null ? List.of(ipAddress) : List.of();

            return CesEvent.builder()

                    .tsUtc(tsUtc)
                    .tsOriginal(eventTime)
                    .tzOffset("+00:00")

                    .sourceType(SourceType.OS)

                    .host(hostname)
                    .user(user)

                    .srcIp(ipAddress)
                    .srcPort(ipPort)

                    .action(action)
                    .object(eventId != null ? "event_" + eventId : null)

                    .result(result)

                    .message(message)

                    .protocol("windows_security")

                    .iocs(Iocs.builder()
                            .ips(ips)
                            .build())

                    .correlationKeys(CorrelationKeys.builder()
                            .user(user)
                            .host(hostname)
                            .srcIp(ipAddress)
                            .build())

                    .rawRef(RawRef.builder()
                            .file(file)
                            .offset(offset)
                            .build())

                    .build();

        } catch (Exception e) {
            System.out.println("Failed parsing windows event: {}" + e.getMessage());
            return null;
        }
    }

    private Instant parseTime(String time) {

        if (time == null) return Instant.now();

        try {
            LocalDateTime ldt = LocalDateTime.parse(time, FORMATTER);
            return ldt.toInstant(ZoneOffset.UTC);
        } catch (Exception e) {
            return Instant.now();
        }
    }

    private String mapAction(Integer eventId) {

        if (eventId == null) return "WINDOWS_EVENT";

        switch (eventId) {

            case 4624:
                return "LOGIN_SUCCESS";

            case 4625:
                return "LOGIN_FAIL";

            case 4688:
                return "PROCESS_CREATE";

            case 4698:
                return "TASK_CREATE";

            case 4720:
                return "USER_CREATE";

            case 4722:
                return "USER_ENABLE";

            case 5140:
                return "FILE_SHARE_ACCESS";

            case 4648:
                return "EXPLICIT_LOGIN";

            default:
                return "WINDOWS_EVENT";
        }
    }

    private ResultType mapResult(Object eventType) {

        if (eventType == null) return ResultType.UNKNOWN;

        String val = eventType.toString().toLowerCase();

        if (val.contains("success"))
            return ResultType.SUCCESS;

        if (val.contains("fail"))
            return ResultType.FAIL;

        return ResultType.UNKNOWN;
    }

    private String safeString(Object obj) {
        return obj != null ? obj.toString() : null;
    }

    private Integer safeInt(Object obj) {

        if (obj == null) return null;

        try {
            return Integer.parseInt(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
