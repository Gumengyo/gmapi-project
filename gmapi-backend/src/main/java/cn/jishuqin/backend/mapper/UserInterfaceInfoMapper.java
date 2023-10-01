package cn.jishuqin.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.jishuqin.common.model.entity.UserInterfaceInfo;
import cn.jishuqin.backend.model.vo.UserInterfaceLeftNumVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
* @author gumeng
* @description 针对表【user_interface_info(用户调用接口关系)】的数据库操作Mapper
* @createDate 2023-05-04 22:28:19
* @Entity cn.jishuqin.project.model.entity.UserInterfaceInfo
*/
public interface UserInterfaceInfoMapper extends BaseMapper<UserInterfaceInfo> {

    List<UserInterfaceInfo> listTopInvokeInterfaceInfo(int limit);

    List<UserInterfaceLeftNumVo> getUserInterfaceLeftNum(Long id);

    /**
     * 调用接口统计
     *
     * @param interfaceInfoId
     * @param userId
     * @return
     */
    int invokeCount(@Param("interfaceInfoId") long interfaceInfoId, @Param("userId") long userId);

}




