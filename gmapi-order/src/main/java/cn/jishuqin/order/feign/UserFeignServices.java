package cn.jishuqin.order.feign;

import cn.jishuqin.common.BaseResponse;
import cn.jishuqin.common.model.dto.LeftNumUpdateDto;
import cn.jishuqin.common.model.entity.InterfaceInfo;
import cn.jishuqin.common.model.vo.LockChargingVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;


/**
 * @author gumeng
 */
@FeignClient(value = "gmapi-backend",url = "http://139.9.212.105:7529/api/")
public interface UserFeignServices {
    /**
     * 获取当前接口的剩余库存
     * @param interfaceId
     * @return
     */
    @GetMapping("/interfaceInfo/getInterfaceInfoById")
    BaseResponse<InterfaceInfo> getInterfaceInfoById(@RequestParam("interfaceId") Long interfaceId);

    /**
     * 更新库存
     * @param lockChargingVo
     * @return
     */
    @PostMapping("/interfaceInfo/updateAvailablePieces")
    BaseResponse<Boolean> updateAvailablePieces(@RequestBody LockChargingVo lockChargingVo);

    /**
     * 远程解锁库存
     * @param lockChargingVo
     * @return
     */
    @PostMapping("/interfaceInfo/unlockAvailablePieces")
    BaseResponse<Boolean> unlockAvailablePieces(@RequestBody LockChargingVo lockChargingVo);

    /**
     * 更新用户剩余可调用次数
     * @param leftNumUpdateDto
     * @return
     */
    @PostMapping("/userInterfaceInfo/updateUserLeftNum")
    BaseResponse<Boolean> updateUserLeftNum(@RequestBody LeftNumUpdateDto leftNumUpdateDto);
}
