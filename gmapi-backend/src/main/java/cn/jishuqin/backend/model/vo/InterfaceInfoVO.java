package cn.jishuqin.backend.model.vo;

import cn.jishuqin.common.model.entity.InterfaceInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 接口信息封装试图
 *
 * @author gumeng
 * @TableName product
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class InterfaceInfoVO extends InterfaceInfo {

    /**
     * 调用次数
     */
    private Long totalNum;

    /**
     *
     * 剩余调用次数
     */

    private Long leftNum;

    private static final long serialVersionUID = 1L;
}