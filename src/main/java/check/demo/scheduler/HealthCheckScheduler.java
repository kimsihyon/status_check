package check.demo.scheduler;

import check.demo.service.HealthCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HealthCheckScheduler {

    private final HealthCheckService service;

    @Scheduled(cron = "*/10 * * * * *") // 매 1초마다 실행
    public void run() {
        String ip = "172.30.29.101"; // 예시 IP
        service.check(1L, ip);
    }
}
