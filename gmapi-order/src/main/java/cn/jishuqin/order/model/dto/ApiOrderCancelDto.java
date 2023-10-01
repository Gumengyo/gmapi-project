package cn.jishuqin.order.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 取消订单dto
 * @author Gumeng
 */
@Data
public class ApiOrderCancelDto implements Serializable {

    /**
     * 接口id
     */
    private Long interfaceId;

    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 购买数量
     */
    private Long orderNum;
}
