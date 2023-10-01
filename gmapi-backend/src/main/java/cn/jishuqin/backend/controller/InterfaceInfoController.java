package cn.jishuqin.backend.controller;

import cn.jishuqin.common.model.vo.LockChargingVo;
import cn.jishuqin.gmapiclientsdk.client.GmApiClient;
import cn.jishuqin.gmapiclientsdk.model.Api;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.jishuqin.common.BaseResponse;
import cn.jishuqin.common.ErrorCode;
import cn.jishuqin.common.ResultUtils;
import cn.jishuqin.common.constant.CommonConstant;
import cn.jishuqin.common.exception.BusinessException;
import cn.jishuqin.common.model.entity.InterfaceInfo;
import cn.jishuqin.common.model.entity.User;
import cn.jishuqin.common.model.entity.UserInterfaceInfo;
import cn.jishuqin.backend.annotation.AuthCheck;
import cn.jishuqin.backend.common.DeleteRequest;
import cn.jishuqin.backend.common.IdRequest;
import cn.jishuqin.backend.model.dto.interfaceinfo.InterfaceInfoAddRequest;
import cn.jishuqin.backend.model.dto.interfaceinfo.InterfaceInfoInvokeRequest;
import cn.jishuqin.backend.model.dto.interfaceinfo.InterfaceInfoQueryRequest;
import cn.jishuqin.backend.model.dto.interfaceinfo.InterfaceInfoUpdateRequest;
import cn.jishuqin.backend.model.enums.InterfaceInfoStatusEnum;
import cn.jishuqin.backend.model.vo.InterfaceInfoVO;
import cn.jishuqin.backend.service.InterfaceInfoService;
import cn.jishuqin.backend.service.UserInterfaceInfoService;
import cn.jishuqin.backend.service.UserService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.jishuqin.common.constant.UserConstant.ADMIN_ROLE;


/**
 * 接口管理
 *
 * @author gumeng
 */
@io.swagger.annotations.Api(tags = "接口管理")
@RestController
@RequestMapping("/interfaceInfo")
@Slf4j
public class InterfaceInfoController {

    @Resource
    private InterfaceInfoService interfaceInfoService;

    @Resource
    private UserService userService;

    @Resource
    private UserInterfaceInfoService userInterfaceInfoService;

    // region 增删改查

    /**
     * 创建
     *
     * @param interfaceInfoAddRequest
     * @param request
     * @return
     */
    @ApiOperation("添加接口")
    @PostMapping("/add")
    @AuthCheck(mustRole = ADMIN_ROLE)
    public BaseResponse<Long> addInterfaceInfo(@RequestBody InterfaceInfoAddRequest interfaceInfoAddRequest, HttpServletRequest request) {
        if (interfaceInfoAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        InterfaceInfo interfaceInfo = new InterfaceInfo();
        BeanUtils.copyProperties(interfaceInfoAddRequest, interfaceInfo);
        // 校验
        interfaceInfoService.validInterfaceInfo(interfaceInfo, true);
        User loginUser = userService.getLoginUser(request);
        interfaceInfo.setUserId(loginUser.getId());
        boolean result = interfaceInfoService.save(interfaceInfo);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        long newInterfaceInfoId = interfaceInfo.getId();
        return ResultUtils.success(newInterfaceInfoId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @ApiOperation("删除接口")
    @AuthCheck(mustRole = ADMIN_ROLE)
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteInterfaceInfo(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        InterfaceInfo oldInterfaceInfo = interfaceInfoService.getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可删除
        if (!oldInterfaceInfo.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = interfaceInfoService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新
     *
     * @param interfaceInfoUpdateRequest
     * @param request
     * @return
     */
    @ApiOperation("更新接口")
    @AuthCheck(mustRole = ADMIN_ROLE)
    @PostMapping("/update")
    public BaseResponse<Boolean> updateInterfaceInfo(@RequestBody InterfaceInfoUpdateRequest interfaceInfoUpdateRequest,
                                            HttpServletRequest request) {
        if (interfaceInfoUpdateRequest == null || interfaceInfoUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        InterfaceInfo interfaceInfo = new InterfaceInfo();
        BeanUtils.copyProperties(interfaceInfoUpdateRequest, interfaceInfo);
        // 参数校验
        interfaceInfoService.validInterfaceInfo(interfaceInfo, false);
        User user = userService.getLoginUser(request);
        long id = interfaceInfoUpdateRequest.getId();
        // 判断是否存在
        InterfaceInfo oldInterfaceInfo = interfaceInfoService.getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可修改
        if (!oldInterfaceInfo.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = interfaceInfoService.updateById(interfaceInfo);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @ApiOperation("根据 id 获取接口信息")
    @GetMapping("/get")
    public BaseResponse<InterfaceInfoVO> getInterfaceInfoById(long id,HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        InterfaceInfo interfaceInfo = interfaceInfoService.getById(id);

        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();

        QueryWrapper<UserInterfaceInfo> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("userId", userId)
                .eq("interfaceInfoId", id);

        UserInterfaceInfo userInterfaceInfo = userInterfaceInfoService.getOne(queryWrapper);
        Long totalNum = userInterfaceInfo.getTotalNum();
        Long leftNum = userInterfaceInfo.getLeftNum();

        InterfaceInfoVO interfaceInfoVO = new InterfaceInfoVO();
        BeanUtils.copyProperties(interfaceInfo, interfaceInfoVO);


        interfaceInfoVO.setTotalNum(totalNum);
        interfaceInfoVO.setLeftNum(leftNum);

        return ResultUtils.success(interfaceInfoVO);
    }

    /**
     * 获取列表（仅管理员可使用）
     *
     * @param interfaceInfoQueryRequest
     * @return
     */
    @ApiOperation("获取接口列表")
    @AuthCheck(mustRole = ADMIN_ROLE)
    @GetMapping("/list")
    public BaseResponse<List<InterfaceInfo>> listInterfaceInfo(InterfaceInfoQueryRequest interfaceInfoQueryRequest) {
        InterfaceInfo interfaceInfoQuery = new InterfaceInfo();
        if (interfaceInfoQueryRequest != null) {
            BeanUtils.copyProperties(interfaceInfoQueryRequest, interfaceInfoQuery);
        }
        QueryWrapper<InterfaceInfo> queryWrapper = new QueryWrapper<>(interfaceInfoQuery);
        List<InterfaceInfo> interfaceInfoList = interfaceInfoService.list(queryWrapper);
        return ResultUtils.success(interfaceInfoList);
    }

    /**
     * 分页获取列表
     *
     * @param interfaceInfoQueryRequest
     * @param request
     * @return
     */
    @ApiOperation("分页获取接口列表")
    @GetMapping("/list/page")
    public BaseResponse<Page<InterfaceInfo>> listInterfaceInfoByPage(InterfaceInfoQueryRequest interfaceInfoQueryRequest, HttpServletRequest request) {
        if (interfaceInfoQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        InterfaceInfo interfaceInfoQuery = new InterfaceInfo();
        BeanUtils.copyProperties(interfaceInfoQueryRequest, interfaceInfoQuery);
        long current = interfaceInfoQueryRequest.getCurrent();
        long size = interfaceInfoQueryRequest.getPageSize();
        String sortField = interfaceInfoQueryRequest.getSortField();
        String sortOrder = interfaceInfoQueryRequest.getSortOrder();
        String description = interfaceInfoQuery.getDescription();
        // content 需支持模糊搜索  
        interfaceInfoQuery.setDescription(null);
        // 限制爬虫
        if (size > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<InterfaceInfo> queryWrapper = new QueryWrapper<>(interfaceInfoQuery);
        queryWrapper.like(StringUtils.isNotBlank(description), "description", description);
        queryWrapper.orderBy(StringUtils.isNotBlank(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        Page<InterfaceInfo> interfaceInfoPage = interfaceInfoService.page(new Page<>(current, size), queryWrapper);
        return ResultUtils.success(interfaceInfoPage);
    }

    // endregion

    /**
     * 发布
     *
     * @param idRequest
     * @param request
     * @return
     */
    @ApiOperation("发布接口")
    @PostMapping("/online")
    @AuthCheck(mustRole = ADMIN_ROLE)
    public BaseResponse<Boolean> onlineInterfaceInfo(@RequestBody IdRequest idRequest,
                                                     HttpServletRequest request) {

        if (idRequest == null || idRequest.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = idRequest.getId();
        // 判断是否存在
        InterfaceInfo oldInterfaceInfo = interfaceInfoService.getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        // 仅本人或管理员可修改
        InterfaceInfo interfaceInfo = new InterfaceInfo();
        interfaceInfo.setId(id);
        interfaceInfo.setStatus(InterfaceInfoStatusEnum.ONLINE.getValue());
        boolean result = interfaceInfoService.updateById(interfaceInfo);
        return ResultUtils.success(result);
    }

    /**
     * 下线
     *
     * @param idRequest
     * @param request
     * @return
     */
    @PostMapping("/offline")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Boolean> offlineInterfaceInfo(@RequestBody IdRequest idRequest,
                                                     HttpServletRequest request) {

        if (idRequest == null || idRequest.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = idRequest.getId();
        // 判断是否存在
        InterfaceInfo oldInterfaceInfo = interfaceInfoService.getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可修改
        InterfaceInfo interfaceInfo = new InterfaceInfo();
        interfaceInfo.setId(id);
        interfaceInfo.setStatus(InterfaceInfoStatusEnum.OFFLINE.getValue());
        boolean result = interfaceInfoService.updateById(interfaceInfo);
        return ResultUtils.success(result);
    }

    /**
     * 获取全站可调用接口数
     * @return
     */
    @GetMapping("/getInterfaceCount")
    public BaseResponse<Long> getInterfaceCount(){
        return ResultUtils.success(interfaceInfoService.count(new QueryWrapper<InterfaceInfo>().eq("status",1)));
    }

    /**
     * 测试调用
     *
     * @param interfaceInfoInvokeRequest
     * @param request
     * @return
     */
    @ApiOperation("测试调用")
    @PostMapping("/invok")
    public BaseResponse<Object> invokeInterfaceInfo(@RequestBody InterfaceInfoInvokeRequest interfaceInfoInvokeRequest,
                                                      HttpServletRequest request) {

        if (interfaceInfoInvokeRequest == null || interfaceInfoInvokeRequest.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = interfaceInfoInvokeRequest.getId();
        String userRequestParams = interfaceInfoInvokeRequest.getUserRequestParams();
        // 判断是否存在
        InterfaceInfo oldInterfaceInfo = interfaceInfoService.getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        if (oldInterfaceInfo.getStatus() == InterfaceInfoStatusEnum.OFFLINE.getValue()){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"接口已关闭");
        }
        User loginUser = userService.getLoginUser(request);
        String accessKey = loginUser.getAccessKey();
        String secretKey = loginUser.getSecretKey();

        String method = oldInterfaceInfo.getMethod();
        String url = oldInterfaceInfo.getUrl();
        Api api = new Api(method,url);
        if ("POST".equals(method) || "post".equals(method)) {
            api.setBody(userRequestParams);
        }else {
            Map<String, String> params = convertParamsToMap(userRequestParams);
            api.setParams(Collections.unmodifiableMap(params));
        }
        GmApiClient gmApiClient = new GmApiClient(accessKey,secretKey);
        String result = gmApiClient.getResult(api);

        return ResultUtils.success(result);
    }

    private static Map<String, String> convertParamsToMap(String params) {
        if (params == null){
            return Collections.emptyMap();
        }
        Map<String, String> paramMap = Arrays.stream(params.split("&"))
                .map(paramPair -> paramPair.split("="))
                .filter(keyValue -> keyValue.length == 2)
                .collect(Collectors.toMap(keyValue -> keyValue[0], keyValue -> keyValue[1]));
        return paramMap;
    }

    /**
     * 获取当前接口的剩余库存
     * @param interfaceId
     * @return
     */
    @GetMapping("/getInterfaceInfoById")
    BaseResponse<InterfaceInfo> getInterfaceInfoById(@RequestParam("interfaceId") Long interfaceId){
        if (interfaceId == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        InterfaceInfo interfaceInfo = interfaceInfoService.getById(interfaceId);
        return ResultUtils.success(interfaceInfo);
    }

    /**
     * 更新库存
     * @param lockChargingVo
     * @return
     */
    @PostMapping("/updateAvailablePieces")
    BaseResponse<Boolean> updateAvailablePieces(@RequestBody LockChargingVo lockChargingVo){
        boolean updated = interfaceInfoService.updateAvailablePieces(lockChargingVo);
        return ResultUtils.success(updated);
    }

    /**
     * 远程解锁库存
     * @param lockChargingVo
     * @return
     */
    @PostMapping("/unlockAvailablePieces")
    BaseResponse<Boolean> unlockAvailablePieces(@RequestBody LockChargingVo lockChargingVo){
        boolean unlocked = interfaceInfoService.unlockAvailablePieces(lockChargingVo);
        return ResultUtils.success(unlocked);
    }
}
