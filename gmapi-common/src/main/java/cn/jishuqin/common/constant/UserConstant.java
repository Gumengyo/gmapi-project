package cn.jishuqin.common.constant;

/**
 * 用户常量
 *
 * @author gumeng
 */
public interface UserConstant {

    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "userLoginState";

    /**
     * 系统用户 id（虚拟用户）
     */
    long SYSTEM_USER_ID = 0;

    //  region 权限

    /**
     * 用户默认头像
     *
     */
    String USER_DEFAULT_AVATAR = "/avatar.jpg";

    /**
     * 登入令牌过期时间
     */

    Long LOGIN_USER_TTL = 10L;


    /**
     * 用户登入key
     */
    String LOGIN_USER_KEY = "api:login:user:";

    /**
     * 默认分配接口次数
     *
     */

    int INTERFACE_COUNT = 10;

    /**
     * 验证码盐
     *
     */
    String LOGIN_CODE_KEY = "api:login:code:";

    /**
     * 验证码失效时间
     *
     */
    Long LOGIN_CODE_TTL = 5L;

    /**
     * 默认权限
     */
    String DEFAULT_ROLE = "user";

    /**
     * 管理员权限
     */
    String ADMIN_ROLE = "admin";

    /**
     *
     * 手机号校验
     */

    String MOBILE_REGEX = "^1[3,4,5,6,7,8,9][0-9]{9}$";

    /**
     * 图片验证码
     */
    String CAPTCHA_PREFIX = "api:captchaId:";

    /**
     * 手机号签名
     */
    String MOBILE_SIGNATURE = "api-mobile-signature";

    // endregion
}
