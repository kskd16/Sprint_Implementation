package com.smartSure.paymentService.service;

import com.smartSure.paymentService.dto.ConfirmPaymentRequest;
import com.smartSure.paymentService.dto.FailPaymentRequest;
import com.smartSure.paymentService.dto.PaymentRequest;
import com.smartSure.paymentService.dto.PaymentResponse;
import com.smartSure.paymentService.entity.Payment;
import com.smartSure.paymentService.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentService.
 * Tests payment initiation (mock mode), confirmation, failure, and retrieval.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks private PaymentService paymentService;

    private Payment mockPayment;

    @BeforeEach
    void setUp() {
        // Inject placeholder keys so mock mode is triggered
        ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "rzp_test_your_key_id");
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "your_razorpay_secret");

        mockPayment = Payment.builder()
                .id(1L)
                .policyId(1L)
                .premiumId(1L)
                .customerId(1L)
                .amount(new BigDecimal("5000.00"))
                .status(Payment.PaymentStatus.PENDING)
                .paymentMethod(Payment.PaymentMethod.UPI)
                .razorpayOrderId("order_mock_123456")
                .build();
    }

    @Test
    @DisplayName("Initiate payment - creates PENDING payment with mock order ID")
    void initiatePayment_createsPendingPayment() {
        PaymentRequest request = new PaymentRequest();
        request.setPolicyId(1L);
        request.setPremiumId(1L);
        request.setAmount(new BigDecimal("5000.00"));
        request.setPaymentMethod(Payment.PaymentMethod.UPI);

        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        PaymentResponse response = paymentService.initiatePayment(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getRazorpayOrderId()).startsWith("order_mock_");
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("Confirm payment - marks SUCCESS and publishes RabbitMQ event")
    void confirmPayment_marksSuccessAndPublishesEvent() {
        ConfirmPaymentRequest req = new ConfirmPaymentRequest();
        req.setRazorpayOrderId("order_mock_123456");
        req.setRazorpayPaymentId("pay_test_abc");
        req.setRazorpaySignature("sig_test");

        when(paymentRepository.findByRazorpayOrderId("order_mock_123456"))
                .thenReturn(Optional.of(mockPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        PaymentResponse response = paymentService.confirmPayment(req);

        assertThat(response).isNotNull();
        verify(paymentRepository).save(argThat(p -> p.getStatus() == Payment.PaymentStatus.SUCCESS));
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("Confirm payment - throws when order not found")
    void confirmPayment_throwsWhenOrderNotFound() {
        ConfirmPaymentRequest req = new ConfirmPaymentRequest();
        req.setRazorpayOrderId("order_nonexistent");

        when(paymentRepository.findByRazorpayOrderId("order_nonexistent"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.confirmPayment(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Payment not found");
    }

    @Test
    @DisplayName("Fail payment - marks FAILED with reason")
    void failPayment_marksFailed() {
        FailPaymentRequest req = new FailPaymentRequest();
        req.setRazorpayOrderId("order_mock_123456");
        req.setReason("Insufficient funds");

        when(paymentRepository.findByRazorpayOrderId("order_mock_123456"))
                .thenReturn(Optional.of(mockPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        PaymentResponse response = paymentService.failPayment(req);

        assertThat(response).isNotNull();
        verify(paymentRepository).save(argThat(p ->
                p.getStatus() == Payment.PaymentStatus.FAILED &&
                "Insufficient funds".equals(p.getFailureReason())));
    }

    @Test
    @DisplayName("Get payment by ID - returns payment when found")
    void getPaymentById_returnsPayment() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(mockPayment));

        PaymentResponse response = paymentService.getPaymentById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Get payment by ID - throws when not found")
    void getPaymentById_throwsWhenNotFound() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Payment not found");
    }

    @Test
    @DisplayName("Get payments by customer - returns customer payments")
    void getPaymentsByCustomer_returnsCustomerPayments() {
        when(paymentRepository.findByCustomerId(1L)).thenReturn(List.of(mockPayment));

        List<PaymentResponse> result = paymentService.getPaymentsByCustomer(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomerId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Get payments by policy - returns policy payments")
    void getPaymentsByPolicy_returnsPolicyPayments() {
        when(paymentRepository.findByPolicyId(1L)).thenReturn(List.of(mockPayment));

        List<PaymentResponse> result = paymentService.getPaymentsByPolicy(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPolicyId()).isEqualTo(1L);
    }
}
