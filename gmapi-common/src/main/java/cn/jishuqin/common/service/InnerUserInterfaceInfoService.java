package cn.jishuqin.common.service;

/**
 *
 */
public interface InnerUserInterfaceInfoService {

    /**
     * 调用接口统计
     * @param interfaceInfoId
     * @param userId
     * @return
     */
    boolean invokeCount(long interfaceInfoId, long userId);

    /**
     * 检查是否有接口调用次数
     * @param interfaceInfoId
     * @param userId
     * @return
     */
    boolean checkCount(long interfaceInfoId,long userId);

}
