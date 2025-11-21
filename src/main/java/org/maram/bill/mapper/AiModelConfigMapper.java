package org.maram.bill.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.maram.bill.entity.AiModelConfig;
import java.util.List;
/**
 * AI模型配置Mapper
 */
@Mapper
public interface AiModelConfigMapper extends BaseMapper<AiModelConfig> {
    
    /**
     * 获取所有可用的AI模型配置
     */
    @Select("SELECT * FROM ai_model_config WHERE status = 'ACTIVE' AND deleted = 0 ORDER BY sort_order")
    List<AiModelConfig> selectActiveModels();
    
    /**
     * 根据模型名称获取配置
     */
    @Select("SELECT * FROM ai_model_config WHERE model_name = #{modelName} AND deleted = 0")
    AiModelConfig selectByModelName(@Param("modelName") String modelName);
    
    /**
     * 获取默认模型
     */
    @Select("SELECT * FROM ai_model_config WHERE is_default = 1 AND status = 'ACTIVE' AND deleted = 0 LIMIT 1")
    AiModelConfig selectDefaultModel();
}