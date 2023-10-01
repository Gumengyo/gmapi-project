package cn.jishuqin.common.constant;

/**
 * @author gumeng
 */
public interface OrderConstant {

    String USER_ORDER_TOKEN_PREFIX = "api:order:token";

    //【0->待付款；1->已完成；2->已关闭；3->无效订单】
    int toBePaid = 0;
    int finish = 1;
    int close = 2;
    int invalid= 3;
}