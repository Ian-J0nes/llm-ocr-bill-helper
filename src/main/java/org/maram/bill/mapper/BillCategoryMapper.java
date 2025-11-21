package org.maram.bill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.maram.bill.entity.BillCategory;

import java.util.List;

/**
 * 账单分类Mapper
 */
@Mapper
public interface BillCategoryMapper extends BaseMapper<BillCategory> {
    
    /**
     * 根据用户ID查询账单分类
     * @param userId 用户ID
     * @return 账单分类列表
     */
    @Select("SELECT * FROM bill_category WHERE (user_id = #{userId} OR user_id IS NULL) AND deleted = 0 ORDER BY sort_order")
    List<BillCategory> selectByUserId(@Param("userId") Long userId);
    
    /**
     * 查询系统预设分类
     * @return 系统分类列表
     */
    @Select("SELECT * FROM bill_category WHERE is_system = 1 AND deleted = 0 ORDER BY sort_order")
    List<BillCategory> selectSystemCategories();
} 