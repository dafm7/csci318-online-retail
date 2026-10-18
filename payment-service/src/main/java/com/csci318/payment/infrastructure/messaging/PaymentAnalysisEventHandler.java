package com.csci318.payment.infrastructure.messaging;

import com.csci318.payment.domain.event.PaymentAnalysisEvent;
import com.csci318.payment.infrastructure.analysis.PaymentAnalysisMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.function.Consumer;

@Configuration
public class PaymentAnalysisEventHandler {
    @Bean
    public Consumer<PaymentAnalysisEvent> paymentAnalysis(PaymentAnalysisMetrics metrics) { return metrics::accept; }
}
