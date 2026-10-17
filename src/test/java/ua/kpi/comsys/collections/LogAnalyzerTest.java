package ua.kpi.comsys.collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogAnalyzerTest {

    private final LogAnalyzer analyzer = new LogAnalyzer();

    private static String createLine(String ip) {
        return ip + " - - [01/May/2025:07:20:10 +0000] \"GET /index.html HTTP/1.1\" 200 9481";
    }

    @Test
    void uniqueIpsParsesExampleFromSpecification() {
        String example = "127.0.0.1 ident alice [01/May/2025:07:20:10 +0000] "
                + "\"GET /index.html HTTP/1.1\" 200 9481";
        assertEquals(Set.of("127.0.0.1"), analyzer.getUniqueIps(List.of(example)));
    }

    @Test
    void uniqueIpsAcceptsDashInBytesField() {
        String dashBytes = "1.1.1.1 - - [10/Oct/2000:13:55:36 -0700] \"GET / HTTP/1.0\" 304 -";
        assertEquals(Set.of("1.1.1.1"), analyzer.getUniqueIps(List.of(dashBytes)));
    }

    @Test
    void uniqueIpsRemovesDuplicates() {
        List<String> logs = List.of(createLine("10.0.0.1"), createLine("10.0.0.2"), createLine("10.0.0.1"));
        assertEquals(Set.of("10.0.0.1", "10.0.0.2"), analyzer.getUniqueIps(logs));
    }

    @Test
    void uniqueIpsOfEmptyListIsEmpty() {
        assertTrue(analyzer.getUniqueIps(List.of()).isEmpty());
    }

    @Test
    void uniqueIpsThrowsOnNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> analyzer.getUniqueIps(null));
        assertEquals("logLines cannot be null", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "not a log line",
            "127.0.0.1 - - 01/May/2025:07:20:10 +0000 \"GET / HTTP/1.1\" 200 1",
            "999.1.1.1 - - [01/May/2025:07:20:10 +0000] \"GET / HTTP/1.1\" 200 1",
            "1.2.3 - - [01/May/2025:07:20:10 +0000] \"GET / HTTP/1.1\" 200 1",
            "1.2.3.4 - - [bad date] \"GET / HTTP/1.1\" 200 1",
            "1.2.3.4 - - [01/May/2025:07:20:10 +0000] GET / HTTP/1.1 200 1",
            "1.2.3.4 - - [01/May/2025:07:20:10 +0000] \"GET / HTTP/1.1\" abc 1",
            "1.2.3.4 - [01/May/2025:07:20:10 +0000] \"GET / HTTP/1.1\" 200 1",
            "1.2.3.4 - - [01/May/2025:07:20:10 +0000] \"GET / HTTP/1.1\" 200"
    })
    void invalidLinesAreIgnoredByBothMethods(String invalid) {
        List<String> logs = List.of(invalid, createLine("5.5.5.5"));
        assertEquals(Set.of("5.5.5.5"), analyzer.getUniqueIps(logs));
        assertEquals(List.of("5.5.5.5"), analyzer.getTopIps(logs, 10));
    }

    @Test
    void nullElementsInsideListAreIgnored() {
        List<String> logs = Arrays.asList(null, createLine("5.5.5.5"), null);
        assertEquals(Set.of("5.5.5.5"), analyzer.getUniqueIps(logs));
        assertEquals(List.of("5.5.5.5"), analyzer.getTopIps(logs, 10));
    }

    @Test
    void topIpsAreSortedByFrequencyDescending() {
        List<String> logs = List.of(
                createLine("1.1.1.1"),
                createLine("2.2.2.2"), createLine("2.2.2.2"),
                createLine("3.3.3.3"), createLine("3.3.3.3"), createLine("3.3.3.3"));
        assertEquals(List.of("3.3.3.3", "2.2.2.2", "1.1.1.1"), analyzer.getTopIps(logs, 3));
    }

    @Test
    void topIpsBreakTiesLexicographically() {
        List<String> logs = List.of(
                createLine("9.0.0.1"), createLine("10.0.0.2"), createLine("8.8.8.8"),
                createLine("200.1.1.1"), createLine("200.1.1.1"));
        assertEquals(List.of("200.1.1.1", "10.0.0.2", "8.8.8.8", "9.0.0.1"),
                analyzer.getTopIps(logs, 4));
    }

    @Test
    void topIpsAreLimitedToTopN() {
        List<String> logs = List.of(
                createLine("1.1.1.1"),
                createLine("2.2.2.2"), createLine("2.2.2.2"),
                createLine("3.3.3.3"), createLine("3.3.3.3"), createLine("3.3.3.3"));
        assertEquals(List.of("3.3.3.3", "2.2.2.2"), analyzer.getTopIps(logs, 2));
        assertEquals(List.of("3.3.3.3"), analyzer.getTopIps(logs, 1));
    }

    @Test
    void topIpsReturnsAllWhenTopNExceedsUniqueCount() {
        List<String> logs = List.of(createLine("2.2.2.2"), createLine("1.1.1.1"), createLine("2.2.2.2"));
        assertEquals(List.of("2.2.2.2", "1.1.1.1"), analyzer.getTopIps(logs, 100));
    }

    @Test
    void topIpsOfEmptyListIsEmpty() {
        assertTrue(analyzer.getTopIps(List.of(), 5).isEmpty());
    }

    @Test
    void topIpsThrowsOnNullLogLines() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> analyzer.getTopIps(null, 3));
        assertEquals("logLines cannot be null", ex.getMessage());
    }

    @Test
    void topIpsChecksNullBeforeTopN() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> analyzer.getTopIps(null, 0));
        assertEquals("logLines cannot be null", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void topIpsThrowsOnNonPositiveTopN(int topN) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> analyzer.getTopIps(List.of(createLine("1.1.1.1")), topN));
        assertEquals("topN must be greater than 0", ex.getMessage());
    }
}