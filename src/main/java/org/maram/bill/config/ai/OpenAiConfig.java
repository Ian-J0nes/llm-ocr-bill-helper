package org.maram.bill.config.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.time.Duration;

/**
 * OpenAI 配置类
 * 主要用于配置 HTTP 客户端的超时时间，避免处理视觉任务时超时
 */
@Configuration
@Slf4j
public class OpenAiConfig {

    /**
     * 读取超时时间（秒）
     * 对于视觉任务（图片识别、OCR 等），处理时间通常在 10-20 秒
     */
    @Value("${spring.ai.openai.read-timeout:30}")
    private int readTimeout;

    /**
     * 连接超时时间（秒）
     */
    @Value("${spring.ai.openai.connect-timeout:10}")
    private int connectTimeout;

    /**
     * 自定义 RestClient 配置
     * Spring AI 会自动使用这个自定义器来配置 OpenAI API 客户端
     */
    @Bean
    public RestClientCustomizer restClientCustomizer() {
        log.info("配置 RestClient 自定义器 - 读取超时: {}秒, 连接超时: {}秒",
                readTimeout, connectTimeout);

        return restClientBuilder -> {
            // 创建自定义的 ClientHttpRequestFactory，设置超时
            ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                    .withConnectTimeout(Duration.ofSeconds(connectTimeout))
                    .withReadTimeout(Duration.ofSeconds(readTimeout));

            ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(settings);

            restClientBuilder.requestFactory(requestFactory);
        };
    }
}
