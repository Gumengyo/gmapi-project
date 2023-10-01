package cn.jishuqin.thirdParty.dto;

import lombok.Data;

/**
 * @author 顾梦
 * @description TODO
 * @since 2023/9/3
 */
@Data
public class AliPay {
    private String traceNo;
    private double totalAmount;
    private String subject;
    private String alipayTraceNo;
}


