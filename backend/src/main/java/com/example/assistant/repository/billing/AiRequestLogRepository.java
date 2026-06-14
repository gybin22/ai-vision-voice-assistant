package com.example.assistant.repository.billing;

import com.example.assistant.entity.billing.AiRequestLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiRequestLogRepository extends JpaRepository<AiRequestLogEntity, Long> {
}
