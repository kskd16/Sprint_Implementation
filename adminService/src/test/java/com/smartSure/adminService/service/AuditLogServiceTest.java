package com.smartSure.adminService.service;

import com.smartSure.adminService.entity.AuditLog;
import com.smartSure.adminService.exception.ResourceNotFoundException;
import com.smartSure.adminService.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditLogService.
 * Tests logging, retrieval by entity, date range, and recent activity.
 */
@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks private AuditLogService auditLogService;

    private AuditLog mockLog;

    @BeforeEach
    void setUp() {
        mockLog = new AuditLog();
        mockLog.setId(1L);
        mockLog.setAdminId(1L);
        mockLog.setAction("APPROVE_CLAIM");
        mockLog.setTargetEntity("Claim");
        mockLog.setTargetId(1L);
        mockLog.setRemarks("All docs verified");
    }

    @Test
    @DisplayName("Log - saves audit log and returns it")
    void log_savesAndReturns() {
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(mockLog);

        AuditLog result = auditLogService.log(1L, "APPROVE_CLAIM", "Claim", 1L, "All docs verified");

        assertThat(result).isNotNull();
        assertThat(result.getAction()).isEqualTo("APPROVE_CLAIM");
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Get all logs - returns all audit logs")
    void getAllLogs_returnsAll() {
        when(auditLogRepository.findAll()).thenReturn(List.of(mockLog));

        List<AuditLog> result = auditLogService.getAllLogs();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Get logs by entity and ID - returns matching logs")
    void getLogsByEntityAndId_returnsMatchingLogs() {
        when(auditLogRepository.findByTargetEntityAndTargetId("Claim", 1L))
                .thenReturn(List.of(mockLog));

        List<AuditLog> result = auditLogService.getLogsByEntityAndId("Claim", 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTargetEntity()).isEqualTo("Claim");
        assertThat(result.get(0).getTargetId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Get logs by date range - returns logs within range")
    void getLogsByDateRange_returnsLogsInRange() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();

        when(auditLogRepository.findByDateRange(from, to)).thenReturn(List.of(mockLog));

        List<AuditLog> result = auditLogService.getLogsByDateRange(from, to);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Get recent logs - returns limited number of logs")
    void getRecentLogs_returnsLimitedLogs() {
        when(auditLogRepository.findRecentLogs(PageRequest.of(0, 5)))
                .thenReturn(List.of(mockLog));

        List<AuditLog> result = auditLogService.getRecentLogs(5);

        assertThat(result).hasSize(1);
        verify(auditLogRepository).findRecentLogs(PageRequest.of(0, 5));
    }

    @Test
    @DisplayName("Get log by ID - returns log when found")
    void getLogById_returnsLog() {
        when(auditLogRepository.findById(1L)).thenReturn(Optional.of(mockLog));

        AuditLog result = auditLogService.getLogById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Get log by ID - throws when not found")
    void getLogById_throwsWhenNotFound() {
        when(auditLogRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auditLogService.getLogById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Get logs by admin - returns logs for specific admin")
    void getLogsByAdmin_returnsAdminLogs() {
        when(auditLogRepository.findByAdminId(1L)).thenReturn(List.of(mockLog));

        List<AuditLog> result = auditLogService.getLogsByAdmin(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAdminId()).isEqualTo(1L);
    }
}
