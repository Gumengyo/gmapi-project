package cn.jishuqin.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.jishuqin.common.model.entity.InterfaceInfo;
import cn.jishuqin.common.model.vo.EchartsVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author gumeng
* @description 针对表【interface_info(接口信息)】的数据库操作Mapper
* @createDate 2023-04-23 19:19:45
* @Entity cn.jishuqin.project.model.entity.InterfaceInfo
*/
public interface InterfaceInfoMapper extends BaseMapper<InterfaceInfo> {
    List<EchartsVo> getInterfaceList(@Param("dateList") List<String> dateList);
}




