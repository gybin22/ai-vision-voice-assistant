package com.example.assistant.repository.billing;

import com.example.assistant.entity.billing.UserTokenAccountEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserTokenAccountRepository extends JpaRepository<UserTokenAccountEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from UserTokenAccountEntity a where a.userId = :userId")
    Optional<UserTokenAccountEntity> findByUserIdForUpdate(@Param("userId") Long userId);
}
