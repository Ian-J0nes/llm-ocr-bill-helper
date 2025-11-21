package org.maram.bill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.maram.bill.entity.Bill;

import java.util.List;

@Mapper
public interface BillMapper extends BaseMapper<Bill> {

    /**
     * 查询用户账单并关联分类信息
     * @param userId 用户ID
     * @return 账单列表（包含分类信息）
     */
    @Select("SELECT b.*, c.category_name, c.category_code, c.description as category_description " +
            "FROM bill b " +
            "LEFT JOIN bill_category c ON b.category_id = c.id " +
            "WHERE b.user_id = #{userId} AND b.deleted = 0 " +
            "ORDER BY b.issue_date DESC, b.create_time DESC")
    List<Bill> selectByUserIdWithCategory(@Param("userId") Long userId);

    /**
     * 分页查询用户账单并关联分类信息
     * @param page 分页参数
     * @param userId 用户ID
     * @return 分页账单列表（包含分类信息）
     */
    @Select("SELECT b.*, c.category_name, c.category_code, c.description as category_description " +
            "FROM bill b " +
            "LEFT JOIN bill_category c ON b.category_id = c.id " +
            "WHERE b.user_id = #{userId} AND b.deleted = 0 " +
            "ORDER BY b.issue_date DESC, b.create_time DESC")
    Page<Bill> selectPageByUserIdWithCategory(Page<Bill> page, @Param("userId") Long userId);

    /**
     * 根据分类ID查询账单数量
     * @param categoryId 分类ID
     * @param userId 用户ID
     * @return 账单数量
     */
    @Select("SELECT COUNT(*) FROM bill WHERE category_id = #{categoryId} AND user_id = #{userId} AND deleted = 0")
    long countByCategoryId(@Param("categoryId") Long categoryId, @Param("userId") Long userId);
}
