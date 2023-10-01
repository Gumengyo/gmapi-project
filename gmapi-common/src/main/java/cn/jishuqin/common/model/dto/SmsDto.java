package cn.jishuqin.common.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author gumeng
 * 发送手机号对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SmsDto implements Serializable {
    /**
     * 手机号
     */
    private String phone;

    /**
     * 验证码
     */
    private String code;
}
