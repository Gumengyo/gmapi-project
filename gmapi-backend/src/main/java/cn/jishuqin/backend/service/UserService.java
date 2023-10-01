package cn.jishuqin.backend.service;


import cn.jishuqin.common.BaseResponse;
import com.baomidou.mybatisplus.extension.service.IService;
import cn.jishuqin.common.model.entity.User;
import cn.jishuqin.backend.model.dto.user.UserBindPhoneRequest;
import cn.jishuqin.backend.model.dto.user.UserLoginRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;

/**
 * 用户服务
 *
 * @author gumeng
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param userPhone 用户手机号
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword,String userPhone,String code);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     *
     * 短信登入
     * @param userLoginRequest
     * @param request
     * @return
     */
    User codeLogin(UserLoginRequest userLoginRequest, HttpServletRequest request);

    /**
     * 获取echarts需要展示的数据
     * @return
     */
    ArrayList<Object> getEchartsData();

    /**
     * 生成图形验证码
     * @param request
     * @param response
     */
    void getCaptcha(HttpServletRequest request, HttpServletResponse response);

    /**
     * 绑定手机号
     * @param userBindPhoneRequest
     * @param request
     * @return
     */
    String bindPhone(UserBindPhoneRequest userBindPhoneRequest, HttpServletRequest request);

    /**
     * 用户忘记密码，返回用户注册时的手机号
     * @param username
     * @return
     */
    String getPassUserType(String username, HttpServletResponse response);

    /**
     * 忘记密码部分-验证手机号和验证码输入是否正确
     * @param code
     * @param request
     * @return
     */
    String authPassUserCode(String code, HttpServletRequest request);

    /**
     * 忘记密码部分-验证手机号和验证码输入是否正确
     * @param request
     * @return
     */
    String sendPassUserCode(HttpServletRequest request);

    /**
     * 修改用户密码
     * @param password
     * @param request
     * @return
     */
    String updateUserPass(String password, HttpServletRequest request);

    /**
     * 发送验证码
     * @param userPhone
     * @return
     */
    BaseResponse<String> captcha(String userPhone);
}
