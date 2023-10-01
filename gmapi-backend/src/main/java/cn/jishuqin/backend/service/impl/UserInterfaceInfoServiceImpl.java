package cn.jishuqin.backend.service.impl;

import cn.jishuqin.common.model.dto.LeftNumUpdateDto;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.jishuqin.common.ErrorCode;
import cn.jishuqin.common.exception.BusinessException;
import cn.jishuqin.common.model.entity.User;
import cn.jishuqin.common.model.entity.UserInterfaceInfo;
import cn.jishuqin.backend.mapper.UserInterfaceInfoMapper;
import cn.jishuqin.backend.model.vo.UserInterfaceLeftNumVo;
import cn.jishuqin.backend.service.UserInterfaceInfoService;
import cn.jishuqin.backend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

/**
* @author gumeng
* @description 针对表【user_interface_info(用户调用接口关系)】的数据库操作Service实现
* @createDate 2023-05-04 22:28:19
*/
@Service
public class UserInterfaceInfoServiceImpl extends ServiceImpl<UserInterfaceInfoMapper, UserInterfaceInfo>
    implements UserInterfaceInfoService{

    @Resource
    private UserService userService;

    @Resource
    private UserInterfaceInfoMapper userInterfaceInfoMapper;

    @Override
    public void validUserInterfaceInfo(UserInterfaceInfo userInterfaceInfo, boolean add) {
        if (userInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 创建时，所有参数必须非空
        if (add) {
            if (userInterfaceInfo.getInterfaceInfoId() <= 0 || userInterfaceInfo.getUserId() <= 0){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"接口或用户不存在");
            }
        }
        if (userInterfaceInfo.getLeftNum() < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "剩余次数不能小于0");
        }
    }

    @Override
    public List<UserInterfaceLeftNumVo> getUserInterfaceLeftNum(HttpServletRequest request) {

        User loginUser = userService.getLoginUser(request);
        Long id = loginUser.getId();
        return userInterfaceInfoMapper.getUserInterfaceLeftNum(id);
    }

    @Override
    @Transactional
    public boolean saveUserLeftNum(long leftNum, long userId, long interfaceId) {
        UserInterfaceInfo userInterfaceInfo = new UserInterfaceInfo();
        userInterfaceInfo.setUserId(userId);
        userInterfaceInfo.setInterfaceInfoId(interfaceId);
        userInterfaceInfo.setLeftNum(leftNum);

        return this.save(userInterfaceInfo);
    }

    @Override
    public boolean updateUserLeftNum(LeftNumUpdateDto leftNumUpdateDto) {
        Long userId = leftNumUpdateDto.getUserId();
        Long interfaceInfoId = leftNumUpdateDto.getInterfaceInfoId();
        Long lockNum = leftNumUpdateDto.getLockNum();

        if (userId == null || interfaceInfoId == null || lockNum == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Long count = this.query().eq("userId", userId)
                .eq("interfaceInfoId", interfaceInfoId)
                .count();

        if (count > 0){
            // 增加剩余用户剩余调用次数
            return this.update(new UpdateWrapper<UserInterfaceInfo>().eq("userId", userId).eq("interfaceInfoId", interfaceInfoId)
                    .setSql("leftNum = leftNum +" + lockNum));
        }else {
            // 用户首次购买接口次数
            return this.saveUserLeftNum(lockNum, userId, interfaceInfoId);
        }
    }


}




