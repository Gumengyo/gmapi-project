package cn.jishuqin.backend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author Gumeng
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LockChargingVo implements Serializable {

    /**
     * 接口id
     */
    private Long interfaceid;

    /**
     * 购买数量
     */
    private Long orderNum;
}
