package com.vulnuris.IngestionService.parser;

import com.vulnuris.IngestionService.model.ces.*;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Component
public class SyslogParser implements LogParser {

    private static final Pattern SYSLOG_PATTERN = Pattern.compile(
            "^<(?<pri>\\d+)>\\d+\\s+" +
                    "(?<ts>\\S+)\\s+" +
                    "(?<host>\\S+)\\s+" +
                    "(?<app>\\S+)\\s+" +
                    "(?<pid>\\S+)\\s+-\\s+-\\s+" +
                    "(?<msg>.*)$"
    );

    private static final Pattern IP_PATTERN =
            Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");

    private static final Pattern USER_PATTERN =
            Pattern.compile("for\\s+(?:invalid user\\s+)?(\\w+)");

    private static final Pattern PORT_PATTERN =
            Pattern.compile("port\\s+(\\d+)");

    @Override
    public Stream<CesEvent> parseStream(InputStream input, String filename) {

        BufferedReader reader = new BufferedReader(new InputStreamReader(input));

        AtomicLong offsetCounter = new AtomicLong(0);

        return reader.lines()
                .map(line -> parseLine(line, filename, offsetCounter.getAndIncrement()))
                .filter(Objects::nonNull);
    }

    private CesEvent parseLine(String line, String file, long offset) {

        if (line == null || line.isBlank()) return null;

        try {

            Matcher m = SYSLOG_PATTERN.matcher(line);

            if (!m.find()) return null;

            String ts = m.group("ts");
            String host = m.group("host");
            String app = m.group("app");
            String msg = m.group("msg");

            Instant tsUtc = Instant.parse(ts);

            String srcIp = extractIp(msg);
            String user = extractUser(msg);
            Integer srcPort = extractPort(msg);
            String protocol = extractProtocol(msg);

            String dstIp = host;
            Integer dstPort = detectServicePort(protocol);

            String action = detectAction(msg);
            ResultType result = detectResult(msg);

            List<String> ips = new ArrayList<>();
            if (srcIp != null) ips.add(srcIp);

            return CesEvent.builder()
                    .tsUtc(tsUtc)
                    .tsOriginal(ts)
                    .tzOffset("+00:00")

                    .sourceType(SourceType.OS)

                    .host(host)
                    .user(user)

                    .srcIp(srcIp)
                    .srcPort(srcPort)

                    .dstIp(dstIp)
                    .dstPort(dstPort)

                    .protocol(protocol)

                    .action(action)
                    .object(app)

                    .result(result)

                    .message(msg)

                    .iocs(Iocs.builder()
                            .ips(ips)
                            .build())

                    .correlationKeys(CorrelationKeys.builder()
                            .host(host)
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
            return null;
        }
    }

    // helper methods unchanged

    private String extractIp(String msg) {
        Matcher ipm = IP_PATTERN.matcher(msg);
        return ipm.find() ? ipm.group() : null;
    }

    private String extractUser(String msg) {

        if (msg == null) return null;

        Matcher um = USER_PATTERN.matcher(msg);

        if (um.find()) {
            return um.group(1);
        }

        return null;
    }

    private Integer extractPort(String msg) {
        Matcher pm = PORT_PATTERN.matcher(msg);

        if (pm.find()) {
            try {
                return Integer.parseInt(pm.group(1));
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }

    private String extractProtocol(String msg) {

        if (msg == null) return null;

        msg = msg.toLowerCase();

        if (msg.contains("ssh"))
            return "ssh";

        if (msg.contains("http"))
            return "http";

        if (msg.contains("https"))
            return "https";

        if (msg.contains("ftp"))
            return "ftp";

        return null;
    }

    private Integer detectServicePort(String protocol) {

        if (protocol == null) return null;

        switch (protocol) {
            case "ssh":
                return 22;
            case "http":
                return 80;
            case "https":
                return 443;
            case "ftp":
                return 21;
            default:
                return null;
        }
    }

    private String detectAction(String msg) {

        if (msg == null) return "UNKNOWN";

        if (msg.contains("Failed password"))
            return "LOGIN_FAIL";

        if (msg.contains("Accepted publickey"))
            return "LOGIN_SUCCESS";

        if (msg.contains("session opened"))
            return "SESSION_OPEN";

        if (msg.contains("logged out"))
            return "LOGOUT";

        if (msg.contains("Firewall rule"))
            return "FIREWALL_CHANGE";

        if (msg.contains("WARNING"))
            return "ALERT";

        if (msg.contains("Memory"))
            return "RESOURCE_ALERT";

        if (msg.contains("Disk"))
            return "RESOURCE_ALERT";

        return "SYSTEM_EVENT";
    }

    private ResultType detectResult(String msg) {

        if (msg == null) return ResultType.UNKNOWN;

        msg = msg.toLowerCase();

        if (msg.contains("failed"))
            return ResultType.FAIL;

        if (msg.contains("accepted"))
            return ResultType.SUCCESS;

        if (msg.contains("accept"))
            return ResultType.SUCCESS;

        return ResultType.UNKNOWN;
    }
}