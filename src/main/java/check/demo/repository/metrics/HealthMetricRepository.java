package check.demo.repository.metrics;

import check.demo.model.metrics.HealthMetric;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthMetricRepository extends JpaRepository<HealthMetric, Long> {}
