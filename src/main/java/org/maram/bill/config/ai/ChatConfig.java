package org.maram.bill.config.ai;

import lombok.Builder;
import lombok.Data;
import org.maram.bill.entity.AiModelConfig;
import org.maram.bill.entity.User;
import org.maram.bill.service.AiModelConfigService;
import org.maram.bill.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 聊天配置
 * 管理 OpenAI 聊天模型的默认配置和用户个性化配置
 */
@Component
@ConfigurationProperties(prefix = "spring.ai.openai.chat.options")
@Data
public class ChatConfig {

    // 默认配置(从配置文件读取)
    private String model = "gpt-4o";
    private Double temperature = 0.3;

    @Autowired
    private UserService userService;

    @Autowired
    private AiModelConfigService aiModelConfigService;


    /**
     * 获取模型的完整配置
     */
    public AiModelConfig getModelConfig(String modelName) {
        return aiModelConfigService.getByModelName(modelName);
    }

    /**
     * 获取用户完整的AI配置信息
     */
    public UserAiConfigInfo getUserAiConfig(String openid) {
        User user = userService.getByOpenid(openid);

        String userModel = this.model;
        if (user != null && user.getAiModel() != null && aiModelConfigService.isModelAvailable(user.getAiModel())) {
            userModel = user.getAiModel();
        }

        Double userTemperature = this.temperature;
        if (user != null && user.getAiTemperature() != null) {
            userTemperature = user.getAiTemperature();
        }

        AiModelConfig modelConfig = getModelConfig(userModel);

        return UserAiConfigInfo.builder()
                .model(userModel)
                .temperature(userTemperature)
                .modelConfig(modelConfig)
                .build();
    }

    /**
     * 用户AI配置信息封装类
     */
    @Data
    @Builder
    public static class UserAiConfigInfo {
        private String model;
        private Double temperature;
        private AiModelConfig modelConfig;
    }
}
