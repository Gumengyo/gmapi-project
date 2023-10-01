package cn.jishuqin.backend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 绑定手机号
 *
 * @author Gumeng
 */
@Data
public class UserBindPhoneRequest implements Serializable {

    /**
     * 用户id
     */
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 手机号
     */
    private String userPhone;

    /**
     * 手机验证码
     */
    private String code;

    /**
     * 图形验证码
     */
    private String captcha;
}
