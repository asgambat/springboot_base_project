package com.example.msbaseprj.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.msbaseprj.entity.OutboxEvent;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
	@NativeQuery("""
			SELECT * FROM outbox_events
			WHERE processed_at IS NULL
			  AND dead_lettered_at IS NULL
			  AND (next_attempt_at IS NULL OR next_attempt_at <= :now)
			  AND (lease_until IS NULL OR lease_until < :now)
			ORDER BY created_at ASC
			LIMIT :batchSize
			FOR UPDATE SKIP LOCKED
			""") // The FOR UPDATE SKIP LOCKED clause ensures that multiple publisher instances
					// don’t compete for the same events
	List<OutboxEvent> findAndLockPendingEvents(@Param("batchSize") int batchSize,
			@Param("now") java.time.LocalDateTime now);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
			UPDATE outbox_events
			SET processed_at = :processedAt, lease_id = NULL, lease_until = NULL, last_error = NULL
			WHERE id = :id AND lease_id = :leaseId AND processed_at IS NULL
			""", nativeQuery = true)
	int markProcessed(@Param("id") UUID id, @Param("leaseId") UUID leaseId,
			@Param("processedAt") java.time.LocalDateTime processedAt);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
			UPDATE outbox_events
			SET attempt_count = :attemptCount, next_attempt_at = :nextAttemptAt,
			    lease_id = NULL, lease_until = NULL, last_error = :lastError
			WHERE id = :id AND lease_id = :leaseId AND processed_at IS NULL
			""", nativeQuery = true)
	int scheduleRetry(@Param("id") UUID id, @Param("leaseId") UUID leaseId, @Param("attemptCount") int attemptCount,
			@Param("nextAttemptAt") java.time.LocalDateTime nextAttemptAt, @Param("lastError") String lastError);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
			UPDATE outbox_events
			SET attempt_count = :attemptCount, dead_lettered_at = :deadLetteredAt,
			    lease_id = NULL, lease_until = NULL, last_error = :lastError
			WHERE id = :id AND lease_id = :leaseId AND processed_at IS NULL
			""", nativeQuery = true)
	int moveToDeadLetter(@Param("id") UUID id, @Param("leaseId") UUID leaseId, @Param("attemptCount") int attemptCount,
			@Param("deadLetteredAt") java.time.LocalDateTime deadLetteredAt, @Param("lastError") String lastError);

	List<OutboxEvent> findTop100ByProcessedAtIsNullOrderByCreatedAtAsc();
}
