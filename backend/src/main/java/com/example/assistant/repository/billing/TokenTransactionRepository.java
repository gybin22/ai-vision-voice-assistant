package com.example.assistant.repository.billing;

import com.example.assistant.entity.billing.TokenTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenTransactionRepository extends JpaRepository<TokenTransactionEntity, Long> {
}
