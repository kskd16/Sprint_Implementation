package com.smartSure.policyService.messaging;

import com.smartSure.policyService.entity.Premium;
import com.smartSure.policyService.repository.PremiumRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final PremiumRepository premiumRepository;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_COMPLETED_QUEUE)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received PaymentCompletedEvent — premiumId={}, policyId={}",
                event.getPremiumId(), event.getPolicyId());

        premiumRepository.findById(event.getPremiumId()).ifPresentOrElse(premium -> {
            if (premium.getStatus() == Premium.PremiumStatus.PENDING
                    || premium.getStatus() == Premium.PremiumStatus.OVERDUE) {

                premium.setStatus(Premium.PremiumStatus.PAID);
                premium.setPaidDate(event.getPaidAt() != null
                        ? event.getPaidAt().toLocalDate()
                        : LocalDate.now());
                premium.setPaymentReference(event.getRazorpayPaymentId());

                if (event.getPaymentMethod() != null) {
                    try {
                        premium.setPaymentMethod(
                                Premium.PaymentMethod.valueOf(event.getPaymentMethod()));
                    } catch (IllegalArgumentException e) {
                        log.warn("Unknown payment method: {}", event.getPaymentMethod());
                    }
                }

                premiumRepository.save(premium);
                log.info("Premium {} marked as PAID for policy {}", event.getPremiumId(), event.getPolicyId());
            } else {
                log.warn("Premium {} already in status {} — skipping", event.getPremiumId(), premium.getStatus());
            }
        }, () -> log.error("Premium {} not found — cannot mark as PAID", event.getPremiumId()));
    }
}
