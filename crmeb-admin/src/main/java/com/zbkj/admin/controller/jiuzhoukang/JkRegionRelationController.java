package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.constants.jiuzhoukang.JkPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkRegion;
import com.zbkj.common.request.jiuzhoukang.*;
import com.zbkj.common.response.jiuzhoukang.JkAdminUserMappingResponse;
import com.zbkj.common.response.jiuzhoukang.JkAgentRelationResponse;
import com.zbkj.common.response.jiuzhoukang.JkRegionAgentResponse;
import com.zbkj.common.response.jiuzhoukang.JkRegionOptionResponse;
import com.zbkj.common.response.jiuzhoukang.JkRegionPathResponse;
import com.zbkj.common.response.jiuzhoukang.JkRegionSearchResponse;
import com.zbkj.common.response.jiuzhoukang.JkRegionTreeNodeResponse;
import com.zbkj.common.response.jiuzhoukang.JkRegionUsageResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
import com.zbkj.service.service.jiuzhoukang.region.*;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/admin/jk")
@Api(tags = "九州康区域、代理关系与后台映射")
public class JkRegionRelationController {
    @Autowired private JkRegionService regionService;
    @Autowired private JkRegionAgentService regionAgentService;
    @Autowired private JkAgentRelationService agentRelationService;
    @Autowired private JkAdminUserMappingService mappingService;
    @Autowired private JkAdminActorService adminActorService;

    @GetMapping("/region/list")
    @PreAuthorize("hasAuthority('"+ JkPermissionCodes.ADMIN_REGION_MANAGE +"')")
    @JkBizPermission(value= JkBizPermissionCodes.REGION_MANAGE)
    public CommonResult<List<JkRegion>> regionList(@RequestParam(required=false)String keywords,@RequestParam(required=false)Boolean status){return CommonResult.success(regionService.list(keywords,status));}

    @GetMapping("/region/children")
    @PreAuthorize("hasAuthority('"+ JkPermissionCodes.ADMIN_REGION_MANAGE +"')")
    @JkBizPermission(value= JkBizPermissionCodes.REGION_MANAGE)
    public CommonResult<List<JkRegionTreeNodeResponse>> regionChildren(@RequestParam(required=false)String parentRegionCode,@RequestParam(required=false)Boolean enabled){return CommonResult.success(regionService.listChildren(parentRegionCode,enabled));}

    @GetMapping("/region/search")
    @PreAuthorize("hasAuthority('"+ JkPermissionCodes.ADMIN_REGION_MANAGE +"')")
    @JkBizPermission(value= JkBizPermissionCodes.REGION_MANAGE)
    public CommonResult<List<JkRegionSearchResponse>> regionSearch(@RequestParam String keyword,@RequestParam(required=false)Integer regionLevel,@RequestParam(required=false)Boolean status,@RequestParam(required=false)Integer limit){return CommonResult.success(regionService.searchRegions(keyword,regionLevel,status,limit));}

    @GetMapping("/region/path/{regionCode}")
    @PreAuthorize("hasAuthority('"+ JkPermissionCodes.ADMIN_REGION_MANAGE +"')")
    @JkBizPermission(value= JkBizPermissionCodes.REGION_MANAGE)
    public CommonResult<JkRegionPathResponse> regionPath(@PathVariable String regionCode){return CommonResult.success(regionService.getRegionPath(regionCode));}

    @GetMapping("/region/options")
    @PreAuthorize("hasAuthority('"+ JkPermissionCodes.ADMIN_REGION_MANAGE +"')")
    @JkBizPermission(value= JkBizPermissionCodes.REGION_MANAGE)
    public CommonResult<List<JkRegionOptionResponse>> regionOptions(@RequestParam(required=false)String parentRegionCode,@RequestParam(required=false)Integer targetLevel,@RequestParam(required=false)Boolean enabled,@RequestParam(required=false)String keyword){return CommonResult.success(regionService.listRegionOptions(parentRegionCode,targetLevel,enabled,keyword));}

    @GetMapping("/region/{regionCode}/usage")
    @PreAuthorize("hasAuthority('"+ JkPermissionCodes.ADMIN_REGION_MANAGE +"')")
    @JkBizPermission(value= JkBizPermissionCodes.REGION_MANAGE)
    public CommonResult<JkRegionUsageResponse> regionUsage(@PathVariable String regionCode){return CommonResult.success(regionService.getRegionUsage(regionCode));}

    @PostMapping("/region/save")
    @PreAuthorize("hasAuthority('"+ JkPermissionCodes.ADMIN_REGION_MANAGE +"')")
    @JkBizPermission(value= JkBizPermissionCodes.REGION_MANAGE)
    public CommonResult<JkRegion> regionSave(@RequestBody @Validated JkRegionSaveRequest request){return CommonResult.success(regionService.save(request,operator()));}

    @PostMapping("/region/status")
    @PreAuthorize("hasAuthority('"+ JkPermissionCodes.ADMIN_REGION_MANAGE +"')")
    @JkBizPermission(value= JkBizPermissionCodes.REGION_MANAGE)
    public CommonResult<Boolean> regionStatus(@RequestParam Long id,@RequestParam boolean status){return CommonResult.success(regionService.updateStatus(id,status,operator()));}

    @GetMapping("/region-agent/list")
    @PreAuthorize("hasAuthority('"+ JkPermissionCodes.ADMIN_REGION_AGENT_MANAGE +"')")
    @JkBizPermission(value= JkBizPermissionCodes.REGION_AGENT_MANAGE)
    public CommonResult<List<JkRegionAgentResponse>> regionAgentList(@RequestParam(required=false)String regionCode,@RequestParam(required=false)Long countyAgentUserId,@RequestParam(required=false)Boolean activeOnly){return CommonResult.success(regionAgentService.list(regionCode,countyAgentUserId,activeOnly));}

    @PostMapping("/region-agent/bind")
    @PreAuthorize("hasAuthority('"+ JkPermissionCodes.ADMIN_REGION_AGENT_MANAGE +"')")
    @JkBizPermission(value= JkBizPermissionCodes.REGION_AGENT_MANAGE)
    public CommonResult<JkRegionAgentResponse> regionAgentBind(@RequestBody @Validated JkRegionAgentBindRequest request){return CommonResult.success(regionAgentService.bind(request,operator()));}

    @PostMapping("/region-agent/invalidate")
    @PreAuthorize("hasAuthority('"+ JkPermissionCodes.ADMIN_REGION_AGENT_MANAGE +"')")
    @JkBizPermission(value= JkBizPermissionCodes.REGION_AGENT_MANAGE)
    public CommonResult<Boolean> regionAgentInvalidate(@RequestParam Long id,@RequestParam(required=false)String reason){return CommonResult.success(regionAgentService.invalidate(id,reason,operator()));}

    @GetMapping("/agent-relation/list")
    @PreAuthorize("hasAuthority('"+ JkPermissionCodes.ADMIN_AGENT_RELATION_MANAGE +"')")
    @JkBizPermission(value= JkBizPermissionCodes.AGENT_RELATION_MANAGE)
    public CommonResult<List<JkAgentRelationResponse>> relationList(@RequestParam(required=false)Long userId,@RequestParam(required=false)Long parentUserId,@RequestParam(required=false)Boolean activeOnly){return CommonResult.success(agentRelationService.list(userId,parentUserId,activeOnly));}

    @PostMapping("/agent-relation/bind")
    @PreAuthorize("hasAuthority('"+ JkPermissionCodes.ADMIN_AGENT_RELATION_MANAGE +"')")
    @JkBizPermission(value= JkBizPermissionCodes.AGENT_RELATION_MANAGE)
    public CommonResult<JkAgentRelationResponse> relationBind(@RequestBody @Validated JkAgentRelationBindRequest request){return CommonResult.success(agentRelationService.bind(request,operator()));}

    @PostMapping("/agent-relation/invalidate")
    @PreAuthorize("hasAuthority('"+ JkPermissionCodes.ADMIN_AGENT_RELATION_MANAGE +"')")
    @JkBizPermission(value= JkBizPermissionCodes.AGENT_RELATION_MANAGE)
    public CommonResult<Boolean> relationInvalidate(@RequestParam Long id,@RequestParam(required=false)String reason){return CommonResult.success(agentRelationService.invalidate(id,reason,operator()));}

    @GetMapping("/admin-mapping/list")
    @PreAuthorize("hasAuthority('"+ JkPermissionCodes.ADMIN_MAPPING_MANAGE +"')")
    @JkBizPermission(value= JkBizPermissionCodes.ADMIN_MAPPING_MANAGE)
    public CommonResult<List<JkAdminUserMappingResponse>> mappingList(@RequestParam(required=false)Integer systemAdminId,@RequestParam(required=false)Long frontUserId){return CommonResult.success(mappingService.list(systemAdminId,frontUserId));}

    @PostMapping("/admin-mapping/save")
    @PreAuthorize("hasAuthority('"+ JkPermissionCodes.ADMIN_MAPPING_MANAGE +"')")
    @JkBizPermission(value= JkBizPermissionCodes.ADMIN_MAPPING_MANAGE)
    public CommonResult<JkAdminUserMappingResponse> mappingSave(@RequestBody @Validated JkAdminUserMappingSaveRequest request){return CommonResult.success(mappingService.save(request,operator()));}

    @PostMapping("/admin-mapping/status")
    @PreAuthorize("hasAuthority('"+ JkPermissionCodes.ADMIN_MAPPING_MANAGE +"')")
    @JkBizPermission(value= JkBizPermissionCodes.ADMIN_MAPPING_MANAGE)
    public CommonResult<Boolean> mappingStatus(@RequestParam Long id,@RequestParam boolean status){return CommonResult.success(mappingService.updateStatus(id,status,operator()));}

    private Long operator(){Long linked=adminActorService.getLinkedFrontUserId(adminActorService.getCurrentAdmin());if(linked!=null)return linked;if(adminActorService.isPlatformSuperAdmin(adminActorService.getCurrentAdmin()))return -Long.valueOf(adminActorService.getCurrentAdmin().getId());throw new IllegalStateException("后台管理员未绑定业务用户");}
}
