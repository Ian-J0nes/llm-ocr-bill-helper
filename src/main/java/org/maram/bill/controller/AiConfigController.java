package org.maram.bill.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.maram.bill.common.security.UserContext;
import org.maram.bill.common.utils.Result;
import org.maram.bill.common.utils.ResultCode;
import org.maram.bill.entity.AiModelConfig;
import org.maram.bill.entity.User;
import org.maram.bill.service.AiModelConfigService;
import org.maram.bill.service.UserService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * AI配置管理控制器
 */
@RestController
@RequestMapping("/ai-config")
@Slf4j
@RequiredArgsConstructor
public class AiConfigController {

    private final AiModelConfigService aiModelConfigService;
    private final UserService userService;
    private final UserContext userContext;

    /**
     * 获取所有可用的AI模型列表
     */
    @GetMapping("/models")
    public Result<List<AiModelConfig>> getAvailableModels() {
        return Result.success(aiModelConfigService.getActiveModels());
    }

    /**
     * 获取用户当前的AI配置
     */
    @GetMapping("/user")
    public Result<User> getUserAiConfig() {
        String openid = userContext.currentOpenid().orElse(null);
        if (openid == null) {
            return Result.unauthorized("用户身份验证失败");
        }
        User user = userService.getByOpenid(openid);
        if (user == null) {
            return Result.error(ResultCode.USER_NOT_FOUND);
        }
        return Result.success(user);
    }

    /**
     * 更新用户的AI配置
     */
    @PutMapping("/user")
    public Result<String> updateUserAiConfig(
            @RequestParam String aiModel,
            @RequestParam Double aiTemperature) {
        String openid = userContext.currentOpenid().orElse(null);
        if (openid == null) {
            return Result.unauthorized("用户身份验证失败");
        }

        // 验证模型是否可用
        if (!aiModelConfigService.isModelAvailable(aiModel)) {
            return Result.badRequest("所选AI模型不可用");
        }

        // 验证温度参数范围
        AiModelConfig modelConfig = aiModelConfigService.getByModelName(aiModel);
        if (modelConfig == null) {
            return Result.error(ResultCode.NOT_FOUND, "AI模型不存在");
        }
        if (aiTemperature < modelConfig.getMinTemperature() || 
            aiTemperature > modelConfig.getMaxTemperature()) {
            return Result.badRequest(
                String.format("温度参数应在 %.2f - %.2f 范围内", 
                    modelConfig.getMinTemperature(), modelConfig.getMaxTemperature()));
        }
        
        // 更新用户配置
        boolean success = userService.updateAiConfig(openid, aiModel, aiTemperature);
        if (success) {
            log.info("用户 [{}] 更新AI配置: 模型={}, 温度={}", openid, aiModel, aiTemperature);
            return Result.success("AI配置更新成功");
        }
        return Result.error("配置更新失败");
    }
}
