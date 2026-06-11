package com.wenxi.neko_ai_agent.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wenxi.neko_ai_agent.constant.QuotaConstant;
import com.wenxi.neko_ai_agent.enums.UserRoleEnum;
import com.wenxi.neko_ai_agent.exception.BusinessException;
import com.wenxi.neko_ai_agent.exception.ErrorCode;
import com.wenxi.neko_ai_agent.mapper.UserMapper;
import com.wenxi.neko_ai_agent.model.dto.user.UserQueryRequest;
import com.wenxi.neko_ai_agent.model.entity.User;
import com.wenxi.neko_ai_agent.model.vo.LoginUserVO;
import com.wenxi.neko_ai_agent.model.vo.UserVO;
import com.wenxi.neko_ai_agent.service.UserService;
import com.wenxi.neko_ai_agent.utils.EmailCodeUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.wenxi.neko_ai_agent.constant.UserConstant.USER_LOGIN_STATE;

/**
* @author kk
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2026-05-03 18:26:25
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {

    /**
     * 用户注册
     * @param userName
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return
     */
    @Override
    public long userRegister(String userName, String userEmail, String userAccount, String userPassword, String checkPassword) {
        // 1.校验参数（结构校验与 @Valid 注解双重保障）
        if (StrUtil.hasBlank(userName, userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userName.length() > 10) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名过长");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次密码不一致");
        }

        // 账号不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账号包含特殊字符");
        }

        // 邮箱为选填，当用户填入了邮箱才会校验
        if (StrUtil.isNotBlank(userEmail)) {
            // 校验邮箱格式是否正确（QQ邮箱）
            boolean validEmail = EmailCodeUtil.isValidEmail(userEmail);
            if(!validEmail){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"邮箱格式不正确");
            }
            // 检查邮箱是否已存在，邮箱重复
            QueryWrapper<User> EmailQueryWrapper = new QueryWrapper<>();
            EmailQueryWrapper.eq("userEmail", userEmail);
            long countOfUserEmail = this.baseMapper.selectCount(EmailQueryWrapper);
            if (countOfUserEmail > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱已存在");
            }
        }

        // 2.检查账号是否已注册（是否重复）
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        long count = this.baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账号已注册");
        }
        // 检查用户名是否已存在
        QueryWrapper<User> nameQueryWrapper = new QueryWrapper<>();
        nameQueryWrapper.eq("userName",userName);
        long nameCount = this.baseMapper.selectCount(nameQueryWrapper);
        if (nameCount > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户名已存在");
        }

        // 3.密码一定要加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 4.数据保存至数据库
        User user = new User();
        user.setUserName(userName);
        user.setUserEmail(StrUtil.isNotBlank(userEmail) ? userEmail : null);
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserRole(UserRoleEnum.USER.getValue());
        user.setDailyQuota(QuotaConstant.DAILY_FREE_QUOTA);
        user.setBonusQuota(0);
        user.setQuotaResetDate(LocalDate.now());
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"注册失败");
        }
        return user.getId();
    }

    /**
     * 用户登录
     * @param userAccount
     * @param userPassword
     * @param request
     * @return
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验参数（结构校验与 @Valid 注解双重保障）
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度不能小于4");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不能小于8");
        }

        // 2. 对用户传递的密码进行加密处理
        String encryptPassword = getEncryptPassword(userPassword);
        // 3. 查询数据库中用户信息是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 不存在则抛异常
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 4. 保存用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, buildSessionUser(user));
        // 返回用户视图
        return this.getLoginUserVO(user);

    }

    /**
     * 用户登录 -> 邮箱登录
     * @param userEmail
     * @param request
     * @return
     */
    @Override
    public LoginUserVO userLoginByEmail(String userEmail, HttpServletRequest request) {
        // 校验参数
        if(StrUtil.isBlank(userEmail) || !EmailCodeUtil.isValidEmail(userEmail)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"邮箱格式错误");
        }
        // 1、根据邮箱查询用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userEmail", userEmail);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 2、用户不存在则抛出异常
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "该邮箱未注册");
        }
        // 3、记录登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, buildSessionUser(user));
        // 4、返回脱敏信息
        LoginUserVO loginUserVO = this.getLoginUserVO(user);
        return loginUserVO;
    }

    /**
     * 用户注销
     * @param request
     * @return
     */
    @Override
    public Boolean userLogout(HttpServletRequest request) {
        // 1. 判断当前是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR,"未登录");
        }
        // 2. 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    /**
     * 获取当前登录的用户信息
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库中查询
        Long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 构建只用于 Session 识别身份的脱敏用户，避免密码哈希进入 HTTP Session。
     */
    private User buildSessionUser(User user) {
        User sessionUser = new User();
        sessionUser.setId(user.getId());
        sessionUser.setUserRole(user.getUserRole());
        return sessionUser;
    }

    /**
     * 获取加密后的密码
     * @param userPassword
     * @return
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        // 加盐，混淆密码
        final String SALT =  "wenxi2003";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    /**
     * 获取脱敏后的用户
     * @param user
     * @return
     */
    @Override
    public UserVO getUserVO(User user) {
        // 1.校验用户信息
        if (user == null) {
            return null;
        }
        // 2.用户信息转换
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO );
        fillQuotaView(user, userVO);
        return userVO;
    }

    /**
     * 获取脱敏后的用户列表
     * @param userList
     * @return
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if(CollUtil.isEmpty(userList)){
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    /**
     * 获取查询条件
     * @param userQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userAvatar = userQueryRequest.getUserAvatar();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        int current = userQueryRequest.getCurrent();
        int pageSize = userQueryRequest.getPageSize();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }


    /**
     * 获取脱敏后的登录用户信息
     * @param user
     * @return
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        // 1. 校验用户信息
        if (user == null) {
            return null;
        }
        // 2. 用户信息转换
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user,loginUserVO);
        fillQuotaView(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 判断用户是否为管理员
     * @param user
     * @return
     */
    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    /**
     * 填充登录用户视图中的积分信息。
     *
     * @param user 用户实体
     * @param loginUserVO 登录用户视图
     */
    private void fillQuotaView(User user, LoginUserVO loginUserVO) {
        int dailyQuota = normalizeQuota(user.getDailyQuota());
        int bonusQuota = normalizeQuota(user.getBonusQuota());
        loginUserVO.setDailyQuota(dailyQuota);
        loginUserVO.setBonusQuota(bonusQuota);
        loginUserVO.setTotalQuota(dailyQuota + bonusQuota);
    }

    /**
     * 填充用户视图中的积分信息。
     *
     * @param user 用户实体
     * @param userVO 用户视图
     */
    private void fillQuotaView(User user, UserVO userVO) {
        int dailyQuota = normalizeQuota(user.getDailyQuota());
        int bonusQuota = normalizeQuota(user.getBonusQuota());
        userVO.setDailyQuota(dailyQuota);
        userVO.setBonusQuota(bonusQuota);
        userVO.setTotalQuota(dailyQuota + bonusQuota);
    }

    /**
     * 规整积分空值。
     *
     * @param quota 积分
     * @return 非空积分
     */
    private int normalizeQuota(Integer quota) {
        return quota == null ? 0 : quota;
    }

}




