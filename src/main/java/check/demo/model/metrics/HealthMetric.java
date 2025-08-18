package check.demo.model.metrics;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "health_metrics")
@Getter
@Setter
public class HealthMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long cctvId;
    private LocalDateTime eventTimestamp;
    private boolean icmpStatus;
    private boolean hlsStatus;
    private String eventCode;

    private Double icmpAvgRttMs;      // RTT 평균 (ms)
    private Double icmpPacketLossPct; // 패킷 손실률 (%)
}
