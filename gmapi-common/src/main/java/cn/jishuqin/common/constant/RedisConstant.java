package cn.jishuqin.common.constant;

/**
 * @author Gumeng
 */
public interface RedisConstant {

    String onlinePageCacheKey = "api:onlinePage:";

    String SEND_ORDER_PREFIX = "api:order:sendOrderSnInfo:";

    String ALIPAY_TRADE_INFO = "api:order:alipayInfo:";

    String ORDER_PAY_SUCCESS_INFO = "api:order:paySuccess:";

    String ORDER_PAY_RABBITMQ = "api:order:payRabbitmq:";
}
