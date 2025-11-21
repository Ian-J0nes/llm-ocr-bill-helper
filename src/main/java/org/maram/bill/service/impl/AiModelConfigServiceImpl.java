package org.maram.bill.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.maram.bill.entity.AiModelConfig;
import org.maram.bill.mapper.AiModelConfigMapper;
import org.maram.bill.service.AiModelConfigService;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * AI模型配置服务实现类
 *
 * @author maram
 * @since 2024-01-01
 */
@Service
public class AiModelConfigServiceImpl extends ServiceImpl<AiModelConfigMapper, AiModelConfig> 
        implements AiModelConfigService {
    
    /**
     * 获取所有激活状态的模型
     *
     * @return 激活状态的模型列表
     */
    @Override
    public List<AiModelConfig> getActiveModels() {
        return baseMapper.selectActiveModels();
    }
    
    /**
     * 根据模型名称获取模型配置
     *
     * @param modelName 模型名称
     * @return 模型配置信息
     */
    @Override
    public AiModelConfig getByModelName(String modelName) {
        return baseMapper.selectByModelName(modelName);
    }
    
    /**
     * 获取默认模型配置
     *
     * @return 默认模型配置
     */
    @Override
    public AiModelConfig getDefaultModel() {
        return baseMapper.selectDefaultModel();
    }
    
    /**
     * 检查模型是否可用
     *
     * @param modelName 模型名称
     * @return 模型是否可用
     */
    @Override
    public boolean isModelAvailable(String modelName) {
        AiModelConfig config = getByModelName(modelName);
        return config != null && "ACTIVE".equals(config.getStatus());
    }
}