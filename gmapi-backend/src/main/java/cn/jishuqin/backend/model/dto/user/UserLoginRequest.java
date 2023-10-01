package cn.jishuqin.backend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求体
 *
 * @author gumeng
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    private String userAccount;

    private String userPassword;

    /**
     *
     *用户手机号
     */

    private String userPhone;

    /**
     *
     * 验证码
     */

    private String code;
}
