package com.smartSure.claimService.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailService.
 * Tests claim decision email content for APPROVED and REJECTED decisions.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;

    @InjectMocks private EmailService emailService;

    @Test
    @DisplayName("Send claim decision email - APPROVED sends correct subject and body")
    void sendClaimDecisionEmail_approved_correctContent() {
        emailService.sendClaimDecisionEmail(
                "rahul@example.com", "Rahul", 1L, "APPROVED");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).contains("rahul@example.com");
        assertThat(sent.getSubject()).contains("1").contains("APPROVED");
        assertThat(sent.getText()).contains("Rahul").contains("APPROVED").contains("settlement");
    }

    @Test
    @DisplayName("Send claim decision email - REJECTED sends correct subject and body")
    void sendClaimDecisionEmail_rejected_correctContent() {
        emailService.sendClaimDecisionEmail(
                "rahul@example.com", "Rahul", 2L, "REJECTED");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).contains("rahul@example.com");
        assertThat(sent.getSubject()).contains("2").contains("REJECTED");
        assertThat(sent.getText()).contains("Rahul").contains("REJECTED").contains("appeal");
    }

    @Test
    @DisplayName("Send claim decision email - mail sender is called exactly once")
    void sendClaimDecisionEmail_calledOnce() {
        emailService.sendClaimDecisionEmail(
                "test@example.com", "Test User", 3L, "APPROVED");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Send claim decision email - recipient is set correctly")
    void sendClaimDecisionEmail_recipientSetCorrectly() {
        String recipient = "customer@example.com";
        emailService.sendClaimDecisionEmail(recipient, "Customer", 4L, "APPROVED");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        assertThat(captor.getValue().getTo()).containsExactly(recipient);
    }
}
