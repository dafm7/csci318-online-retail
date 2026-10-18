package com.csci318.order.infrastructure.messaging;

import com.csci318.order.domain.event.OrderAnalysisEvent;
import com.csci318.order.infrastructure.analysis.OrderAnalysisMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.function.Consumer;

@Configuration
public class OrderAnalysisEventHandler {
    @Bean
    public Consumer<OrderAnalysisEvent> orderAnalysis(OrderAnalysisMetrics metrics) { return metrics::accept; }
}
