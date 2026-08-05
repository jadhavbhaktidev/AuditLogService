package com.auditlogservice.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.auditlogservice.domain.AuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditRecordRepository extends JpaRepository<AuditRecord, Long>, JpaSpecificationExecutor<AuditRecord> {

    @Query("select coalesce(max(a.sequenceNumber), 0) from AuditRecord a")
    Long findMaxSequenceNumber();

    List<AuditRecord> findAllByOrderBySequenceNumberAsc();

    Optional<AuditRecord> findBySequenceNumber(Long sequenceNumber);

    @Modifying
    @Query("update AuditRecord a set a.archivedAt = :archivedAt where a.archivedAt is null and a.eventTimestamp < :cutoffEpochMillis")
    int archiveOlderThan(@Param("cutoffEpochMillis") long cutoffEpochMillis,
                         @Param("archivedAt") OffsetDateTime archivedAt);
}
