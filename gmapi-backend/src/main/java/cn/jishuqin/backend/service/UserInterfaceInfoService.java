package cn.jishuqin.backend.service;

import cn.jishuqin.common.model.dto.LeftNumUpdateDto;
import com.baomidou.mybatisplus.extension.service.IService;
import cn.jishuqin.common.model.entity.UserInterfaceInfo;
import cn.jishuqin.backend.model.vo.UserInterfaceLeftNumVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author gumeng
* @description 针对表【user_interface_info(用户调用接口关系)】的数据库操作Service
* @createDate 2023-05-04 22:28:19
*/
public interface UserInterfaceInfoService extends IService<UserInterfaceInfo> {

    void validUserInterfaceInfo(UserInterfaceInfo userInterfaceInfo, boolean add);

    /**
     * 获取已购买接口
     * @param request
     * @return
     */
    List<UserInterfaceLeftNumVo> getUserInterfaceLeftNum(HttpServletRequest request);


    /**
     * 用户购买调用次数
     * @param leftNum
     * @param userId
     * @param interfaceId
     * @return
     */
    boolean saveUserLeftNum(long leftNum,long userId,long interfaceId);


    /**
     * 更新用户可调用次数
     * @param leftNumUpdateDto
     * @return
     */
    boolean updateUserLeftNum(LeftNumUpdateDto leftNumUpdateDto);
}
