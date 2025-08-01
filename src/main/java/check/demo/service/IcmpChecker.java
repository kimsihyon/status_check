@Component
public class IcmpChecker {
    public IcmpResult check(String ip) {
        // 실제 ICMP 체크 구현
    }

    public static class IcmpResult {
        private boolean success;
        private double avgRtt;
        private double minRtt;
        private double maxRtt;
        private double packetLossPct;
        // getters/setters
    }
}
