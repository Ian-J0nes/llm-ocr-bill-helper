package org.maram.bill.service;
import org.maram.bill.entity.AiModelConfig;
import java.util.List;
/**
 * AI模型配置服务接口
 */
public interface AiModelConfigService {
    
    /**
     * 获取所有可用的AI模型
     */
    List<AiModelConfig> getActiveModels();
    
    /**
     * 根据模型名称获取配置
     */
    AiModelConfig getByModelName(String modelName);
    
    /**
     * 获取默认模型
     */
    AiModelConfig getDefaultModel();
    
    /**
     * 验证模型是否可用
     */
    boolean isModelAvailable(String modelName);
}