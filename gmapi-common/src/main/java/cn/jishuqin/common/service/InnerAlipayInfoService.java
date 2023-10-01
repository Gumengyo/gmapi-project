package cn.jishuqin.common.service;

import cn.jishuqin.common.model.entity.AlipayInfo;

/**
* @author 顾梦
* @description 针对表【alipay_info(订单支付信息)】的数据库操作Service
* @createDate 2023-09-04 20:22:59
*/
public interface InnerAlipayInfoService {

    /**
     * 根据订单号获取支付宝订单信息
     * @param orderSn
     * @return
     */
    AlipayInfo getByOrderSn(String orderSn);
}
