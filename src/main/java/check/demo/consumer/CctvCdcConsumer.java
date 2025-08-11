package check.demo.consumer;

import check.demo.model.Cctv;
import check.demo.repository.CctvRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CctvCdcConsumer {

    private final CctvRepository repository;
    // 스프링 빈 주입을 쓰고 싶으면 생성자 주입으로 바꿔도 OK
    private final ObjectMapper om = new ObjectMapper();

    /**
     * Debezium + ExtractNewRecordState(rewrite) 가정:
     * - key: "123" 또는 {"id":123}
     * - value: 행 JSON (insert/update), 삭제 시 value=null
     */
    @Transactional
    @KafkaListener(
            topics = "cdc.prod.cctv",
            groupId = "${spring.kafka.consumer.group-id:cctv-cdc-consumer}"
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        final String key = record.key();
        final String value = record.value();

        try {
            if (key == null) {
                log.warn("[CDC] skip: null key (value={})", value);
                return;
            }

            Long id = tryParseIdFromKey(key);
            JsonNode v = null;

            if (value != null) {
                v = om.readTree(value);
                // key에서 id 못 꺼냈다면 value에서 보조 추출 시도
                if (id == null) id = tryParseIdFromValue(v);
            } else {
                // delete 이벤트(value=null)
                if (id == null) {
                    log.warn("[CDC] skip delete: unable to parse id from key={}", key);
                    return;
                }
            }

            if (id == null) {
                log.warn("[CDC] skip: unable to resolve id (key={}, value={})", key, value);
                return;
            }

            // 삭제 처리
            if (value == null) {
                repository.deleteById(id);
                log.info("[CDC] delete id={}", id);
                return;
            }

            // 업서트 처리
            String ip = text(v, "ip_address");
            if (ip == null || ip.isBlank()) {
                // cctv_read.ip_address 가 NOT NULL 이므로 방어
                log.warn("[CDC] skip upsert: id={} empty ip_address (value={})", id, value);
                return; // 정책에 따라 deleteById(id)로 바꿔도 됨
            }

            Cctv row = repository.findById(id).orElseGet(Cctv::new);
            row.setId(id);
            row.setIpAddress(ip);

            repository.save(row);
            log.info("[CDC] upsert id={} ip={}", id, ip);

        } catch (Exception e) {
            log.error("[CDC] error key={} value={}", key, value, e);
            // 필요하면 DLQ(사이드 토픽)로 전송하는 로직 추가 가능
        }
    }

    private Long tryParseIdFromKey(String key) {
        try {
            // 케이스1: "123"
            return Long.valueOf(key);
        } catch (NumberFormatException ignored) {
            try {
                // 케이스2: {"id":123}
                JsonNode n = om.readTree(key);
                if (n != null && n.has("id") && n.get("id").isNumber()) {
                    return n.get("id").asLong();
                }
            } catch (Exception ignored2) {
                // not JSON
            }
            return null;
        }
    }

    private Long tryParseIdFromValue(JsonNode v) {
        return (v != null && v.has("id") && v.get("id").isNumber()) ? v.get("id").asLong() : null;
    }

    private String text(JsonNode n, String f) {
        return (n != null && n.hasNonNull(f)) ? n.get(f).asText() : null;
    }
}
