package io.wobline.payments.application.projection;

import io.wobline.payments.domain.PaymentEvent;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletionStage;

import org.apache.pekko.Done;
import org.apache.pekko.projection.eventsourced.EventEnvelope;
import org.apache.pekko.projection.javadsl.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaymentViewEventHandler extends Handler<EventEnvelope<PaymentEvent>> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final PaymentViewRepository paymentViewRepository;

    public PaymentViewEventHandler(PaymentViewRepository paymentViewRepository) {
        this.paymentViewRepository = paymentViewRepository;
    }

    @Override
    public CompletionStage<Done> process(EventEnvelope<PaymentEvent> paymentEventEventEnvelope) {
        log.info("Processing: {}", paymentEventEventEnvelope.event());
        return switch (paymentEventEventEnvelope.event()) {
            case PaymentEvent.PaymentProcessed processed -> paymentViewRepository.save(
                    processed
                            .createdAt()
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")),
                    processed.paymentId(),
                    processed.cardNumber(),
                    processed.expiryDate(),
                    processed.cvv(),
                    processed.amount(),
                    processed.currency(),
                    processed.merchantId(),
                    processed.status());
            case PaymentEvent.PaymentStatusUpdated processed -> paymentViewRepository.save(
                    processed
                            .createdAt()
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")),
                    processed.paymentId(),
                    processed.cardNumber(),
                    processed.expiryDate(),
                    processed.cvv(),
                    processed.amount(),
                    processed.currency(),
                    processed.merchantId(),
                    processed.status());
        };

    }
}
