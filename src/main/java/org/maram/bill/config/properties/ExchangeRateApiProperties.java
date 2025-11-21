package org.maram.bill.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "rate-api")
@Data
public class ExchangeRateApiProperties {
    private String url;
    private String baseCurrency;
    private List<String> targetCurrencies;
}