package check.demo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HealthMetricEventDto {
    private Long cctvId;
    private LocalDateTime timestamp;
    private boolean icmpStatus;
    private boolean hlsStatus;
    private String eventCode;

    private Double icmpAvgRttMs;
    private Double icmpPacketLossPct;
}
