package com.zbkj.admin.controller.jiuzhoukang;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.constants.jiuzhoukang.JkPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkBusinessRole;
import com.zbkj.common.model.jiuzhoukang.JkStockAccount;
import com.zbkj.common.model.jiuzhoukang.JkUserBusinessRole;
import com.zbkj.common.model.system.SystemAdmin;
import com.zbkj.common.model.user.User;
import com.zbkj.common.response.jiuzhoukang.JkOptionResponse;
import com.zbkj.common.response.jiuzhoukang.JkRegionOptionResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.dao.jiuzhoukang.JkBusinessRoleDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockAccountDao;
import com.zbkj.service.dao.jiuzhoukang.JkUserBusinessRoleDao;
import com.zbkj.service.service.SystemAdminService;
import com.zbkj.service.service.UserService;
import com.zbkj.service.service.jiuzhoukang.region.JkRegionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/admin/jk/options")
public class JkManagementOptionController {
    @Autowired private UserService userService;
    @Autowired private SystemAdminService systemAdminService;
    @Autowired private JkUserBusinessRoleDao userRoleDao;
    @Autowired private JkBusinessRoleDao roleDao;
    @Autowired private JkStockAccountDao stockAccountDao;
    @Autowired private JkRegionService regionService;

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_MANAGEMENT_OPTION_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.MANAGEMENT_OPTION_VIEW)
    public CommonResult<List<JkOptionResponse>> users(@RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) String roleCode,
                                                       @RequestParam(defaultValue = "20") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        Set<Integer> roleUsers = null;
        Map<Long, String> roleByUser = new HashMap<>();
        if (StrUtil.isNotBlank(roleCode)) {
            List<JkUserBusinessRole> bindings = userRoleDao.selectList(new LambdaQueryWrapper<JkUserBusinessRole>()
                    .eq(JkUserBusinessRole::getRoleCode, roleCode)
                    .eq(JkUserBusinessRole::getAuditStatus, JkBizConstants.AUDIT_STATUS_EFFECTIVE)
                    .eq(JkUserBusinessRole::getFreezeStatus, false)
                    .eq(JkUserBusinessRole::getIsDeleted, false));
            roleUsers = bindings.stream().map(v -> v.getUserId().intValue()).collect(Collectors.toCollection(LinkedHashSet::new));
            for (JkUserBusinessRole binding : bindings) roleByUser.putIfAbsent(binding.getUserId(), binding.getRoleCode());
            if (roleUsers.isEmpty()) return CommonResult.success(Collections.<JkOptionResponse>emptyList());
        }
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<User>().eq(User::getStatus, true).orderByDesc(User::getUid);
        if (roleUsers != null) query.in(User::getUid, roleUsers);
        if (StrUtil.isNotBlank(keyword)) {
            String key = keyword.trim();
            if (key.matches("\\d+")) {
                query.and(q -> q.like(User::getNickname, key).or().like(User::getRealName, key)
                        .or().like(User::getPhone, key).or().eq(User::getUid, Integer.valueOf(key)));
            } else {
                query.and(q -> q.like(User::getNickname, key).or().like(User::getRealName, key).or().like(User::getPhone, key));
            }
        }
        List<User> users = userService.list(query.last("limit " + safeLimit));
        Map<String, String> roleNames = roleDao.selectList(new LambdaQueryWrapper<JkBusinessRole>().eq(JkBusinessRole::getIsDeleted, false))
                .stream().collect(Collectors.toMap(JkBusinessRole::getRoleCode, JkBusinessRole::getRoleName, (a, b) -> a));
        List<JkOptionResponse> rows = new ArrayList<>();
        for (User user : users) {
            String code = roleByUser.get(Long.valueOf(user.getUid()));
            String name = StrUtil.blankToDefault(user.getRealName(), user.getNickname());
            String roleName = code == null ? null : roleNames.getOrDefault(code, code);
            rows.add(new JkOptionResponse().setValue(String.valueOf(user.getUid()))
                    .setLabel(name + " / " + StrUtil.blankToDefault(user.getPhone(), "无手机号") + "（ID:" + user.getUid() + "）")
                    .setExtra(roleName).setPhone(user.getPhone()).setRoleCode(code).setRoleName(roleName).setDisabled(false));
        }
        return CommonResult.success(rows);
    }

    @GetMapping("/admins")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_MANAGEMENT_OPTION_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.MANAGEMENT_OPTION_VIEW)
    public CommonResult<List<JkOptionResponse>> admins(@RequestParam(required = false) String keyword,
                                                        @RequestParam(defaultValue = "20") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        LambdaQueryWrapper<SystemAdmin> query = new LambdaQueryWrapper<SystemAdmin>()
                .eq(SystemAdmin::getStatus, true).eq(SystemAdmin::getIsDel, false).orderByDesc(SystemAdmin::getId);
        if (StrUtil.isNotBlank(keyword)) {
            String key = keyword.trim();
            query.and(q -> q.like(SystemAdmin::getAccount, key).or().like(SystemAdmin::getRealName, key).or().like(SystemAdmin::getPhone, key));
        }
        List<JkOptionResponse> rows = systemAdminService.list(query.last("limit " + safeLimit)).stream().map(admin ->
                new JkOptionResponse().setValue(String.valueOf(admin.getId()))
                        .setLabel(StrUtil.blankToDefault(admin.getRealName(), admin.getAccount()) + " / " + admin.getAccount() + "（ID:" + admin.getId() + "）")
                        .setPhone(admin.getPhone()).setDisabled(false)).collect(Collectors.toList());
        return CommonResult.success(rows);
    }

    @GetMapping("/regions")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_MANAGEMENT_OPTION_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.MANAGEMENT_OPTION_VIEW)
    public CommonResult<List<JkOptionResponse>> regions(@RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) String parentRegionCode,
                                                         @RequestParam(required = false) Integer targetLevel,
                                                         @RequestParam(required = false) Boolean enabled) {
        List<JkRegionOptionResponse> options = regionService.listRegionOptions(parentRegionCode, targetLevel, enabled == null ? true : enabled, keyword);
        List<JkOptionResponse> rows = options.stream().map(region ->
                new JkOptionResponse().setValue(region.getValue()).setLabel(region.getLabel() + "（" + region.getValue() + "）")
                        .setExtra(String.valueOf(region.getRegionLevel())).setRegionCode(region.getValue()).setDisabled(region.getDisabled()))
                .collect(Collectors.toList());
        return CommonResult.success(rows);
    }

    @GetMapping("/stock-accounts")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_MANAGEMENT_OPTION_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.MANAGEMENT_OPTION_VIEW)
    public CommonResult<List<JkOptionResponse>> stockAccounts(@RequestParam(required = false) String keyword,
                                                               @RequestParam(defaultValue = "30") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        LambdaQueryWrapper<JkStockAccount> query = new LambdaQueryWrapper<JkStockAccount>()
                .eq(JkStockAccount::getIsDeleted, false)
                .eq(JkStockAccount::getStatus, true)
                .orderByAsc(JkStockAccount::getAccountType)
                .orderByDesc(JkStockAccount::getId);
        if (StrUtil.isNotBlank(keyword)) {
            String key = keyword.trim();
            query.and(q -> q.like(JkStockAccount::getAccountNo, key)
                    .or().like(JkStockAccount::getOwnerName, key)
                    .or().like(JkStockAccount::getRoleCode, key)
                    .or().like(JkStockAccount::getRegionCode, key));
        }
        List<JkOptionResponse> rows = stockAccountDao.selectList(query.last("limit " + safeLimit)).stream()
                .map(account -> new JkOptionResponse()
                        .setValue(String.valueOf(account.getId()))
                        .setLabel(StrUtil.blankToDefault(account.getOwnerName(), account.getAccountNo())
                                + " / " + account.getAccountType() + "（" + account.getAccountNo() + "）")
                        .setExtra(account.getRegionCode())
                        .setRoleCode(account.getRoleCode())
                        .setRegionCode(account.getRegionCode())
                        .setDisabled(false))
                .collect(Collectors.toList());
        return CommonResult.success(rows);
    }
}