package com.auditlogservice.repository;

import java.util.List;

import com.auditlogservice.domain.AuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditRecordRepository extends JpaRepository<AuditRecord, Long>, JpaSpecificationExecutor<AuditRecord> {

    @Query("select coalesce(max(a.sequenceNumber), 0) from AuditRecord a")
    Long findMaxSequenceNumber();

    List<AuditRecord> findAllByOrderBySequenceNumberAsc();
}
