package ua.kpi.comsys.collections;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LogAnalyzer {

    private static final String OCTET = "(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)";
    private static final String IP = OCTET + "(?:\\." + OCTET + "){3}";
    private static final String DATE = "\\d{2}/[A-Za-z]{3}/\\d{4}(?::\\d{2}){3} [+-]\\d{4}";

    private static final Pattern CLF_LINE = Pattern.compile(
            "(" + IP + ") \\S+ \\S+ \\[" + DATE + "] \"[^\"]*\" \\d{3} (?:\\d+|-)");

    public Set<String> getUniqueIps(List<String> logLines)  {
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
            if (matcher.matches()) {
                ips.add(matcher.group(1));
            }
        }
        return ips;
    }
}