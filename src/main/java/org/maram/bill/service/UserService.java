package org.maram.bill.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.Map;
import org.maram.bill.entity.User;

/**
 * 用户服务接口，定义用户相关的业务操作。
 */
public interface UserService {

    /**
     * 根据用户ID获取用户信息。
     *
     * @param id 用户ID
     * @return 对应的用户账户信息；如果找不到则返回 null
     */
    User getById(Long id);

    /**
     * 根据用户名获取用户信息。
     *
     * @param username 用户名
     * @return 对应的用户账户信息；如果找不到则返回 null
     */
    User getByUsername(String username);

    /**
     * 根据微信用户的 OpenID 获取用户信息。
     *
     * @param openid 微信用户的 OpenID
     * @return 对应的用户账户信息；如果找不到则返回 null
     */
    User getByOpenid(String openid);

    /**
     * 获取所有用户的列表。
     *
     * @return 包含所有用户账户信息的列表
     */
    List<User> list();

    /**
     * 更新指定ID的用户信息。
     * 注意：此方法不应用于更新密码。
     *
     * @param userAccount 包含待更新信息的用户DTO，必须包含用户ID
     * @return 如果更新成功返回 true，否则返回 false
     */
    boolean updateUserById(User userAccount);

    /**
     * 根据用户ID删除用户（通常为逻辑删除）。
     *
     * @param id 待删除用户的ID
     * @return 如果删除成功返回 true，否则返回 false
     */
    boolean removeById(Long id);

    /**
     * 处理微信用户登录请求。
     * 如果用户首次登录，会自动为其创建账户。
     *
     * @param code 微信登录凭证 code
     * @return 登录成功后整理的登录信息（包含 token ）
    */
    Map<String, Object> wxLogin(String code);

    /**
     * 分页查询用户信息。
     *
     * @param page         分页参数对象，包含当前页码和每页大小
     * @param queryWrapper Mybatis Plus 的查询条件构造器，用于构建复杂的查询条件
     * @return 包含用户账户信息的分页结果对象
     */
    Page<User> page(Page<User> page, QueryWrapper<User> queryWrapper);

    /**
     * 根据微信用户的 OpenID 获取用户ID。
     *
     * @param openid 微信用户的 OpenID
     * @return 对应用户的ID；如果找不到则返回 null
     */
    Long getUserIdByOpenid(String openid);

    /**
     * 更新用户AI配置
     */
    boolean updateAiConfig(String openid, String aiModel, Double aiTemperature);

    /**
     * 根据用户ID获取OpenID。
     * @param userId 用户ID
     * @return 对应的OpenID；如果找不到则返回null
     */
    String getOpenidByUserId(Long userId);

}
