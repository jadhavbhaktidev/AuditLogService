package com.auditlogservice.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.auditlogservice.domain.RedactionAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RedactionAuditRepository extends JpaRepository<RedactionAudit, Long> {

    List<RedactionAudit> findBySequenceNumberInOrderBySequenceNumberAscApprovedAtDesc(Collection<Long> sequenceNumbers);

    Optional<RedactionAudit> findTopBySequenceNumberOrderByApprovedAtDesc(Long sequenceNumber);
}
