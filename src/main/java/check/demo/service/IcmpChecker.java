package check.demo.service;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Component
public class IcmpChecker {

    public IcmpResult check(String ip) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder builder;

            if (os.contains("win")) {
                builder = new ProcessBuilder("ping", "-n", "2", "-w", "1000", ip);
            } else {
                builder = new ProcessBuilder("ping", "-c", "2", "-W", "1", ip);
            }

            builder.redirectErrorStream(true);
            Process process = builder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            boolean receivedReply = false;

            while ((line = reader.readLine()) != null) {
                if (line.toLowerCase().contains("ttl=")) {
                    receivedReply = true;
                }
            }

            int exitCode = process.waitFor();
            return new IcmpResult(exitCode == 0 && receivedReply);
        } catch (Exception e) {
            return new IcmpResult(false);
        }
    }

    public static class IcmpResult {
        private final boolean success;

        public IcmpResult(boolean success) {
            this.success = success;
        }

        public boolean isSuccess() {
            return success;
        }
    }
}
