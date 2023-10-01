package cn.jishuqin.common.constant;

/**
 * 分布式锁常量
 * @author gumeng
 */
public interface LockConstant {

    String sms_fail_lock = "sendSms:fail:lock";

    String sms_waitToLong_lock = "sendSms:wait:lock";

    String interface_onlinePage_lock = "interface:onlinePage:lock";

    String order_fail_lock = "sendOrderInfo:fail:lock";

    String order_pay_success = "order:paySuccess:lock";
}
