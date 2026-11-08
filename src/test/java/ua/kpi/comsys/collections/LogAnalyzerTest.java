
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

    private static final String TIMESTAMP = "[01/May/2025:07:20:10 +0000]";
    private static final String REQUEST = "\"GET /index.html HTTP/1.1\"";

    private final LogAnalyzer analyzer = new LogAnalyzer();

    private static String createLine(String ip) {
        return createLine(ip, "-", "-", TIMESTAMP, REQUEST, "200", "9481");
    }

    private static String createLine(String ip, String ident, String user,
                                     String timestamp, String request,
                                     String status, String bytes) {
        return ip + " " + ident + " " + user + " " + timestamp + " "
                + request + " " + status + " " + bytes;
    }

    private void assertAccepted(String logLine) {
        assertEquals(Set.of("1.2.3.4"), analyzer.getUniqueIps(List.of(logLine)));
        assertEquals(List.of("1.2.3.4"), analyzer.getTopIps(List.of(logLine), 1));
    }

    private void assertIgnored(String logLine) {
        assertTrue(analyzer.getUniqueIps(List.of(logLine)).isEmpty());
        assertTrue(analyzer.getTopIps(List.of(logLine), 1).isEmpty());
    }

    @Test
    void uniqueIpsParsesExampleFromSpecification() {
        String example = createLine("1.2.3.4", "ident", "alice", TIMESTAMP, REQUEST, "200", "9481");
        assertAccepted(example);
    }

    @Test
    void uniqueIpsAcceptsDashInBytesField() {
        String line = createLine("1.2.3.4", "-", "-", "[10/Oct/2000:13:55:36 -0700]",
                "\"GET / HTTP/1.0\"", "304", "-");
        assertAccepted(line);
    }

    @Test
    void acceptsDashesInAllFieldsExceptIp() {
        assertAccepted("1.2.3.4 - - - - - -");
    }

    @Test
    void acceptsBracketsAroundMissingTimestamp() {
        assertAccepted("1.2.3.4 - - [-] - - -");
    }

    @Test
    void acceptsBothQuotedAndUnquotedMissingRequest() {
        assertAccepted("1.2.3.4 - - - - - -");
        assertAccepted("1.2.3.4 - - - \"-\" - -");
        assertAccepted("1.2.3.4 - - [-] \"-\" - -");
    }

    @Test
    void countsBothMissingRequestFormsInTopIps() {
        List<String> logs = List.of(
                createLine("1.2.3.4", "-", "-", TIMESTAMP, "-", "200", "1"),
                createLine("1.2.3.4", "-", "-", TIMESTAMP, "\"-\"", "200", "1"),
                createLine("5.5.5.5"));
        assertEquals(Set.of("1.2.3.4", "5.5.5.5"), analyzer.getUniqueIps(logs));
        assertEquals(List.of("1.2.3.4", "5.5.5.5"), analyzer.getTopIps(logs, 2));
    }

    @Test
    void acceptsOneMissingFieldAtATime() {
        assertAccepted(createLine("1.2.3.4", "-", "alice", TIMESTAMP, REQUEST, "200", "1"));
        assertAccepted(createLine("1.2.3.4", "ident", "-", TIMESTAMP, REQUEST, "200", "1"));
        assertAccepted(createLine("1.2.3.4", "ident", "alice", "-", REQUEST, "200", "1"));
        assertAccepted(createLine("1.2.3.4", "ident", "alice", TIMESTAMP, "-", "200", "1"));
        assertAccepted(createLine("1.2.3.4", "ident", "alice", TIMESTAMP, "\"-\"", "200", "1"));
        assertAccepted(createLine("1.2.3.4", "ident", "alice", TIMESTAMP, REQUEST, "-", "1"));
        assertAccepted(createLine("1.2.3.4", "ident", "alice", TIMESTAMP, REQUEST, "200", "-"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-", "ident", "bob", "john.smith", "user_1", "admin@example.com"})
    void acceptsValidIdentValues(String ident) {
        assertAccepted(createLine("1.2.3.4", ident, "alice", TIMESTAMP, REQUEST, "200", "1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-", "alice", "john.smith", "user_1", "admin@example.com"})
    void acceptsValidUserValues(String user) {
        assertAccepted(createLine("1.2.3.4", "-", user, TIMESTAMP, REQUEST, "200", "1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"!invalid", "bad name", "[ident]", ""})
    void ignoresInvalidIdentValues(String ident) {
        assertIgnored(createLine("1.2.3.4", ident, "alice", TIMESTAMP, REQUEST, "200", "1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"!invalid", "bad name", "[user]", ""})
    void ignoresInvalidUserValues(String user) {
        assertIgnored(createLine("1.2.3.4", "-", user, TIMESTAMP, REQUEST, "200", "1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "TRACE", "CONNECT"})
    void acceptsSupportedHttpMethods(String method) {
        String target = method.equals("CONNECT") ? "example.com:443" : "/api/users?id=1";
        assertAccepted(createLine("1.2.3.4", "-", "-", TIMESTAMP,
                "\"" + method + " " + target + " HTTP/1.1\"", "200", "1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "\"GET / HTTP/1.0\"",
            "\"GET /index.html?lang=uk HTTP/1.1\"",
            "\"GET https://example.com/path?q=1 HTTP/2\"",
            "\"OPTIONS * HTTP/1.1\"",
            "\"CONNECT example.com:443 HTTP/1.1\"",
            "\"POST /api/users HTTP/2.0\"",
            "\"GET / HTTP/3\""
    })
    void acceptsSupportedRequestForms(String request) {
        assertAccepted(createLine("1.2.3.4", "-", "-", TIMESTAMP, request, "200", "1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "\"FETCH / HTTP/1.1\"",
            "\"get / HTTP/1.1\"",
            "\"GET index.html HTTP/1.1\"",
            "\"GET / FTP/1.1\"",
            "\"GET / HTTP/x\"",
            "\"GET / HTTP/1.1.1\"",
            "\"GET /\"",
            "\"GET / HTTP/1.1 extra\"",
            "GET / HTTP/1.1",
            "\"GET /bad path HTTP/1.1\"",
            "\"GET / HTTP/1.1",
            "\"--\""
    })
    void ignoresInvalidHttpRequests(String request) {
        assertIgnored(createLine("1.2.3.4", "-", "-", TIMESTAMP, request, "200", "1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"100", "201", "301", "404", "500", "599", "-"})
    void acceptsValidOrMissingStatuses(String status) {
        assertAccepted(createLine("1.2.3.4", "-", "-", TIMESTAMP, REQUEST, status, "1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "99", "600", "999", "1000", "abc", "200 OK"})
    void ignoresInvalidHttpStatuses(String status) {
        assertIgnored(createLine("1.2.3.4", "-", "-", TIMESTAMP, REQUEST, status, "1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "12345", "-"})
    void acceptsValidOrMissingByteCount(String bytes) {
        assertAccepted(createLine("1.2.3.4", "-", "-", TIMESTAMP, REQUEST, "200", bytes));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "1.5", "abc", ""})
    void ignoresInvalidByteCount(String bytes) {
        assertIgnored(createLine("1.2.3.4", "-", "-", TIMESTAMP, REQUEST, "200", bytes));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "[29/Feb/2024:12:30:45 +0000]",
            "[31/Dec/2025:23:59:59 -0700]",
            "[01/Jan/2000:00:00:00 +0530]",
            "-", "[-]"
    })
    void acceptsValidOrMissingTimestamps(String timestamp) {
        assertAccepted(createLine("1.2.3.4", "-", "-", timestamp, REQUEST, "200", "1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "[29/Feb/2025:12:30:45 +0000]",
            "[31/Apr/2025:12:30:45 +0000]",
            "[00/May/2025:12:30:45 +0000]",
            "[32/May/2025:12:30:45 +0000]",
            "[01/Foo/2025:12:30:45 +0000]",
            "[01/May/2025:24:00:00 +0000]",
            "[01/May/2025:12:60:00 +0000]",
            "[01/May/2025:12:30:60 +0000]",
            "[01/May/2025:12:30:45 +2500]",
            "[01/May/2025:12:30:45 +0060]",
            "[bad date]", "bad date"
    })
    void ignoresInvalidTimestamps(String timestamp) {
        assertIgnored(createLine("1.2.3.4", "-", "-", timestamp, REQUEST, "200", "1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.0.0.0", "255.255.255.255", "192.168.1.1"})
    void acceptsValidIpv4Addresses(String ip) {
        assertEquals(Set.of(ip), analyzer.getUniqueIps(List.of(createLine(ip))));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "-", "256.1.1.1", "999.1.1.1", "1.2.3", "1.2.3.4.5",
            "01.2.3.4", "2001:db8::1", "abc", ""
    })
    void ignoresMissingOrInvalidIpAddresses(String ip) {
        assertIgnored(createLine(ip, "-", "-", TIMESTAMP, REQUEST, "200", "1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "   ", "not a log line",
            "127.0.0.1 - - 01/May/2025:07:20:10 +0000 \"GET / HTTP/1.1\" 200 1",
            "127.0.0.1 - - [01/May/2025:07:20:10 +0000] GET / HTTP/1.1 200 1",
            "127.0.0.1 - [01/May/2025:07:20:10 +0000] \"GET / HTTP/1.1\" 200 1",
            "127.0.0.1 - - [01/May/2025:07:20:10 +0000] \"GET / HTTP/1.1\" 200",
            "127.0.0.1 - - [01/May/2025:07:20:10 +0000] \"GET / HTTP/1.1\" 200 1 extra"
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
