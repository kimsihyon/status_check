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
                builder = new ProcessBuilder("ping", "-n", "4", "-w", "1000", ip);
            } else {
                builder = new ProcessBuilder("ping", "-c", "4", "-W", "1", ip);
            }

            builder.redirectErrorStream(true);
            Process process = builder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            double avgRtt = -1;
            double packetLoss = -1;

            while ((line = reader.readLine()) != null) {
                if (line.contains("packets transmitted") || line.contains("패킷을 보냈고")) {
                    // Linux example: "4 packets transmitted, 4 received, 0% packet loss"
                    String[] parts = line.split(",");
                    if (parts.length >= 3) {
                        packetLoss = Double.parseDouble(parts[2].replaceAll("[^0-9.]", ""));
                    }
                } else if (line.contains("rtt min/avg/max") || line.contains("평균 =")) {
                    // Linux example: "rtt min/avg/max/mdev = 0.123/0.456/0.789/0.111 ms"
                    String[] parts = line.split("=");
                    if (parts.length >= 2) {
                        String[] rttParts = parts[1].trim().split("/");
                        avgRtt = Double.parseDouble(rttParts[1]);
                    }
                }
            }

            int exitCode = process.waitFor();
            boolean success = exitCode == 0;
            return new IcmpResult(success, avgRtt, packetLoss);

        } catch (Exception e) {
            e.printStackTrace();
            return null; // ping 자체 실패 (예: DNS 해석 실패)
        }
    }

    public static class IcmpResult {
        private final boolean success;
        private final double avgRtt;
        private final double packetLossPct;

        public IcmpResult(boolean success, double avgRtt, double packetLossPct) {
            this.success = success;
            this.avgRtt = avgRtt;
            this.packetLossPct = packetLossPct;
        }

        public boolean isSuccess() { return success; }
        public double getAvgRtt() { return avgRtt; }
        public double getPacketLossPct() { return packetLossPct; }
    }
}
