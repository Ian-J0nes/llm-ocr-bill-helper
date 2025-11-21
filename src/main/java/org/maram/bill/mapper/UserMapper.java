package org.maram.bill.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.Optional;

/**
 * 用户账户Mapper (更新版)
 */
@Mapper
public interface UserMapper extends BaseMapper<org.maram.bill.entity.User> {

    /**
     * 根据openid查询用户(包含AI配置)
     */
    @Select("SELECT * FROM user WHERE openid = #{openid} AND deleted = 0")
    Optional<org.maram.bill.entity.User> findByOpenid(@Param("openid") String openid);

    /**
     * 更新用户AI配置
     */
    @Update("UPDATE user SET ai_model = #{aiModel}, ai_temperature = #{aiTemperature}, " +
            "ai_config_updated_at = NOW() " +
            "WHERE openid = #{openid} AND deleted = 0")
    int updateAiConfig(@Param("openid") String openid,
                       @Param("aiModel") String aiModel,
                       @Param("aiTemperature") Double aiTemperature);

}