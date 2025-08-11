package check.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cctv_read")
@Getter @Setter
public class Cctv {
    @Id
    private Long id;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;
}
