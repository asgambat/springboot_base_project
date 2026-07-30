package com.example.msbaseprj.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import com.example.msbaseprj.entity.PaymentIdempotency;

public interface PaymentIdempotencyRepository extends JpaRepository<PaymentIdempotency, String> {
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Transactional
	@Query("""
			UPDATE PaymentIdempotency payment
			SET payment.processingLeaseUntil = :leaseUntil
			WHERE payment.idempotencyKey = :idempotencyKey
			  AND payment.requestFingerprint = :requestFingerprint
			  AND payment.completedAt IS NULL
			  AND payment.processingLeaseUntil < :now
			""")
	int reclaimExpiredLease(@Param("idempotencyKey") String idempotencyKey,
			@Param("requestFingerprint") String requestFingerprint, @Param("now") Instant now,
			@Param("leaseUntil") Instant leaseUntil);
}