package com.auditlogservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.auditlogservice.repository.AuditRecordRepository;
import com.auditlogservice.repository.RedactionAuditRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
class AuditLogServiceApplicationTests {

    @MockBean
    private AuditRecordRepository auditRecordRepository;

    @MockBean
    private RedactionAuditRepository redactionAuditRepository;

    @Test
    void contextLoads() {
    }
}
