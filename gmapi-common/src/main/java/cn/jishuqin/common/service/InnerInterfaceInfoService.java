package cn.jishuqin.common.service;

import cn.jishuqin.common.model.entity.InterfaceInfo;
import cn.jishuqin.common.model.vo.LockChargingVo;

/**
 *
 */
public interface InnerInterfaceInfoService {

    /**
     * 从数据库中查询模拟接口是否存在（请求路径、请求方法、请求参数）
     */
    InterfaceInfo getInterfaceInfo(String path, String method);

}
