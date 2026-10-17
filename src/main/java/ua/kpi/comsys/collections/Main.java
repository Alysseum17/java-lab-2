package ua.kpi.comsys.collections;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        LogAnalyzer analyzer = new LogAnalyzer();

        List<String> logs = List.of(
                "127.0.0.1 ident alice [01/May/2025:07:20:10 +0000] \"GET /index.html HTTP/1.1\" 200 9481",
                "10.0.0.2 - - [01/May/2025:07:20:11 +0000] \"GET /about.html HTTP/1.1\" 200 512",
                "10.0.0.2 - - [01/May/2025:07:20:12 +0000] \"POST /login HTTP/1.1\" 302 -",
                "9.0.0.1 - - [01/May/2025:07:20:13 +0000] \"GET / HTTP/1.1\" 200 1024",
                "127.0.0.1 - bob [01/May/2025:07:20:14 +0000] \"GET /style.css HTTP/1.1\" 200 2048",
                "this is not a log line",
                "999.1.1.1 - - [01/May/2025:07:20:15 +0000] \"GET / HTTP/1.1\" 200 1");

        System.out.println("Unique IPs: " + analyzer.getUniqueIps(logs));
        System.out.println("Top 2 IPs: " + analyzer.getTopIps(logs, 2));
        System.out.println("Top 10 IPs: " + analyzer.getTopIps(logs, 10) + "\n");

        try {
            analyzer.getUniqueIps(null);
        } catch (IllegalArgumentException e) {
            System.out.println("getUniqueIps(null) -> " + e.getMessage());
        }

        try {
            analyzer.getTopIps(null, 3);
        } catch (IllegalArgumentException e) {
            System.out.println("getTopIps(null, 3) -> " + e.getMessage());
        }

        try {
            analyzer.getTopIps(logs, 0);
        } catch (IllegalArgumentException e) {
            System.out.println("getTopIps(logs, 0) -> " + e.getMessage());
        }
    }
}