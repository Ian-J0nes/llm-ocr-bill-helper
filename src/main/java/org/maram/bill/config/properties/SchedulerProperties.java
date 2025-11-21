package org.maram.bill.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "scheduler")
@Data
public class SchedulerProperties {
    private String updateExchangeRatesCron;
}