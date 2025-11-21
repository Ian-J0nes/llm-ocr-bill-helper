package org.maram.bill.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * 简单封装微信登录接口，专注 code -> openid 的转换。
 */
@Component
@Slf4j
public class WxAuthClient {

    private static final String WX_LOGIN_URL = "https://api.weixin.qq.com/sns/jscode2session";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String appid;
    private final String secret;

    public WxAuthClient(RestTemplateBuilder restTemplateBuilder,
                        ObjectMapper objectMapper,
                        @Value("${wechat.appid}") String appid,
                        @Value("${wechat.secret}") String secret) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(java.time.Duration.ofSeconds(5))
                .setReadTimeout(java.time.Duration.ofSeconds(10))
                .build();
        this.objectMapper = objectMapper;
        this.appid = appid;
        this.secret = secret;
    }

    public String exchangeCodeForOpenid(String code) {
        if (!StringUtils.hasText(code)) {
            log.warn("尝试使用空的 code 换取 openid");
            throw new IllegalArgumentException("code 不能为空");
        }
        
        log.debug("开始换取 openid, code: {}", code.substring(0, Math.min(10, code.length())) + "...");
        
        try {
            String url = UriComponentsBuilder.fromHttpUrl(WX_LOGIN_URL)
                    .queryParam("appid", appid)
                    .queryParam("secret", secret)
                    .queryParam("js_code", code)
                    .queryParam("grant_type", "authorization_code")
                    .toUriString();
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("微信登录接口返回非成功状态: {}", response.getStatusCode());
                throw new IllegalStateException("微信登录接口调用失败");
            }
            
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            
            if (rootNode.has("errcode")) {
                int errcode = rootNode.get("errcode").asInt();
                String errmsg = rootNode.path("errmsg").asText("未知错误");
                log.error("微信登录接口返回错误, errcode: {}, errmsg: {}", errcode, errmsg);
                throw new IllegalStateException("微信登录失败: " + errmsg);
            }
            
            if (rootNode.has("openid")) {
                String openid = rootNode.get("openid").asText();
                log.info("成功获取 openid");
                return openid;
            }
            
            log.error("微信登录接口未返回 openid 或 errcode: {}", rootNode);
            throw new IllegalStateException("微信登录接口返回异常响应");
            
        } catch (RestClientException ex) {
            log.error("调用微信登录接口失败", ex);
            throw new IllegalStateException("调用微信登录接口失败", ex);
        } catch (IOException ex) {
            log.error("解析微信登录响应失败", ex);
            throw new IllegalStateException("解析微信登录响应失败", ex);
        }
    }
}
