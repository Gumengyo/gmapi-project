package cn.jishuqin.common.constant;

/**
 * rabbitmq 常量
 * @author Gumeng
 */
public interface RabbitMqConstant {
    String sms_queue = "api-sms-queue";
    String sms_exchange= "sms.exchange";
    String sms_delay_queue = "sms.delay.queue";

    String sms_routingKey= "sms-send";

    String SMS_HASH_PREFIX = "api:sms_hash_";

    String MQ_PRODUCER="api:mq:producer:fail";

    String order_exchange = "order.exchange";

    String order_delay_queue = "order.delay.queue";

    String order_queue = "api-order-queue";

    String order_routingKey= "order-send";

    String order_pay_success = "order-pay-success";

}
