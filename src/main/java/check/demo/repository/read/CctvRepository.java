package check.demo.repository.read;

import check.demo.model.read.Cctv;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CctvRepository extends JpaRepository<Cctv, Long> {}
