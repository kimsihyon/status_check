package check.demo.repository;

import check.demo.model.Cctv;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CctvRepository extends JpaRepository<Cctv, Long> {}
