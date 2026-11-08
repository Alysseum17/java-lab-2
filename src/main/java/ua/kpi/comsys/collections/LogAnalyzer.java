
package ua.kpi.comsys.collections;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LogAnalyzer {

    private static final String OCTET = "(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)";
    private static final String IP = OCTET + "(?:\\." + OCTET + "){3}";

    private static final String IDENT = "(?:-|[A-Za-z0-9_][A-Za-z0-9._@-]*)";
    private static final String USER = "(?:-|[A-Za-z0-9_][A-Za-z0-9._@-]*)";

    private static final String DAY = "(?:0[1-9]|[12]\\d|3[01])";
    private static final String MONTH = "(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)";
    private static final String DATE = DAY + "/" + MONTH + "/\\d{4}";
    private static final String TIME = "(?:[01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d";
    private static final String TIME_ZONE = "[+-](?:[01]\\d|2[0-3])[0-5]\\d";
    private static final String DATE_TIME = DATE + ":" + TIME + " " + TIME_ZONE;

    private static final String TIMESTAMP =
            "(?:\\[(?<timestamp>" + DATE_TIME + "|-)\\]|-)";

    private static final String METHOD =
            "(?:GET|HEAD|POST|PUT|PATCH|DELETE|OPTIONS|TRACE|CONNECT)";
    private static final String TARGET_CHAR = "[^\\s\"\\x00-\\x1F\\x7F]";
    private static final String TARGET =
            "(?:/" + TARGET_CHAR + "*|https?://" + TARGET_CHAR
                    + "+|\\*|[A-Za-z0-9.-]+:\\d{1,5})";
    private static final String HTTP_VERSION = "HTTP/\\d+(?:\\.\\d+)?";

    private static final String REQUEST =
            "(?:\"(?:" + METHOD + " " + TARGET + " " + HTTP_VERSION + "|-)\"|-)";

    private static final String STATUS = "(?:[1-5]\\d{2}|-)";
    private static final String BYTES = "(?:\\d+|-)";

    private static final Pattern CLF_LINE = Pattern.compile(
            "(?<ip>" + IP + ") " + IDENT + " " + USER + " " + TIMESTAMP
                    + " " + REQUEST + " " + STATUS + " " + BYTES);

    private static final DateTimeFormatter LOG_DATE_FORMAT = DateTimeFormatter
            .ofPattern("dd/MMM/uuuu:HH:mm:ss Z", Locale.ENGLISH)
            .withResolverStyle(ResolverStyle.STRICT);

    public Set<String> getUniqueIps(List<String> logLines) {
        requireLogLines(logLines);
        return new LinkedHashSet<>(extractIps(logLines));
    }

    public List<String> getTopIps(List<String> logLines, int topN) {
        requireLogLines(logLines);
        if (topN <= 0) {
            throw new IllegalArgumentException("topN must be greater than 0");
        }

        Map<String, Long> frequencies = extractIps(logLines).stream()
                .collect(Collectors.groupingBy(ip -> ip, Collectors.counting()));

        return frequencies.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(topN)
                .map(Map.Entry::getKey)
                .toList();
    }

    private static void requireLogLines(List<String> logLines) {
        if (logLines == null) {
            throw new IllegalArgumentException("logLines cannot be null");
        }
    }

    private static List<String> extractIps(List<String> logLines) {
        List<String> ips = new ArrayList<>();
        for (String line : logLines) {
            if (line == null) {
                continue;
            }

            Matcher matcher = CLF_LINE.matcher(line);
            if (matcher.matches() && isValidTimestamp(matcher.group("timestamp"))) {
                ips.add(matcher.group("ip"));
            }
        }
        return ips;
    }

    private static boolean isValidTimestamp(String timestamp) {
        if (timestamp == null || timestamp.equals("-")) {
            return true;
        }

        try {
            OffsetDateTime.parse(timestamp, LOG_DATE_FORMAT);
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }
}
