package com.zbkj.service.service.impl.jiuzhoukang.region;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.constants.SysConfigConstants;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.model.jiuzhoukang.JkBusinessRole;
import com.zbkj.common.model.jiuzhoukang.JkRegion;
import com.zbkj.common.model.jiuzhoukang.JkUserBusinessRole;
import com.zbkj.common.model.user.User;
import com.zbkj.common.response.jiuzhoukang.JkAgentRelationResponse;
import com.zbkj.common.response.jiuzhoukang.JkOptionResponse;
import com.zbkj.common.response.jiuzhoukang.JkPromotionQrcodeResponse;
import com.zbkj.common.response.jiuzhoukang.JkTeamSummaryResponse;
import com.zbkj.common.utils.QRCodeUtil;
import com.zbkj.service.dao.jiuzhoukang.JkBusinessRoleDao;
import com.zbkj.service.dao.jiuzhoukang.JkRegionDao;
import com.zbkj.service.dao.jiuzhoukang.JkUserBusinessRoleDao;
import com.zbkj.service.service.SystemConfigService;
import com.zbkj.service.service.UserService;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContextService;
import com.zbkj.service.service.jiuzhoukang.region.JkAgentRelationService;
import com.zbkj.service.service.jiuzhoukang.region.JkAgentTeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class JkAgentTeamServiceImpl implements JkAgentTeamService {
    @Autowired private JkAgentRelationService relationService;
    @Autowired private SystemConfigService systemConfigService;
    @Autowired private UserService userService;
    @Autowired private JkUserBusinessRoleDao userRoleDao;
    @Autowired private JkBusinessRoleDao roleDao;
    @Autowired private JkRegionDao regionDao;
    @Autowired private JkUserContextService contextService;

    @Override
    public JkTeamSummaryResponse summary(Long userId) {
        List<JkAgentRelationResponse> current = relationService.list(userId, null, true);
        List<JkAgentRelationResponse> direct = relationService.list(null, userId, true);
        List<JkAgentRelationResponse> history = relationService.list(userId, null, false);
        Set<Long> allTeamUserIds = collectAllTeamUserIds(userId);
        Date todayStart = startOfToday();
        Date monthStart = startOfMonth();
        int todayNew = 0;
        int monthNew = 0;
        for (JkAgentRelationResponse relation : direct) {
            Date effective = relation.getEffectiveTime();
            if (effective != null && !effective.before(todayStart)) todayNew++;
            if (effective != null && !effective.before(monthStart)) monthNew++;
            enrichRelationMember(relation);
        }

        JkTeamSummaryResponse response = new JkTeamSummaryResponse();
        response.setUserId(userId);
        response.setCurrentRelation(current.isEmpty() ? null : current.get(0));
        response.setDirectTeamCount(direct.size());
        response.setTotalTeamCount(allTeamUserIds.size());
        response.setTeamCount(allTeamUserIds.size());
        response.setTodayNewCount(todayNew);
        response.setMonthNewCount(monthNew);
        response.setDirectTeam(direct);
        response.setRelationHistory(history);
        return response;
    }

    /**
     * 直属成员详情只返回能够从现有真实表中确定的数据。
     * 佣金记录缺少“来源用户”快照，无法准确反推某个成员给上级带来的贡献，因此显式返回不可用状态，
     * 而不是把成员本人佣金或随机数字冒充成贡献数据。
     */
    @Override
    public Map<String, Object> memberDetail(Long currentUserId, Long memberUserId) {
        if (currentUserId == null || memberUserId == null) throw new IllegalArgumentException("成员参数不能为空");
        List<JkAgentRelationResponse> relations = relationService.list(memberUserId, currentUserId, true);
        if (relations.isEmpty()) throw new IllegalArgumentException("该用户不是您的直属团队成员");
        JkAgentRelationResponse relation = relations.get(0);

        User user = userService.getById(memberUserId.intValue());
        if (user == null) throw new IllegalArgumentException("团队成员不存在");

        JkUserBusinessRole role = userRoleDao.selectOne(new LambdaQueryWrapper<JkUserBusinessRole>()
                .eq(JkUserBusinessRole::getUserId, memberUserId)
                .eq(JkUserBusinessRole::getAuditStatus, JkBizConstants.AUDIT_STATUS_EFFECTIVE)
                .eq(JkUserBusinessRole::getIsDeleted, false)
                .orderByDesc(JkUserBusinessRole::getIsPrimary)
                .orderByDesc(JkUserBusinessRole::getId)
                .last("limit 1"));
        JkBusinessRole roleDefinition = role == null ? null : roleDao.selectOne(new LambdaQueryWrapper<JkBusinessRole>()
                .eq(JkBusinessRole::getRoleCode, role.getRoleCode())
                .eq(JkBusinessRole::getIsDeleted, false)
                .last("limit 1"));
        JkRegion region = role == null || StrUtil.isBlank(role.getRegionCode()) ? null
                : regionDao.selectOne(new LambdaQueryWrapper<JkRegion>()
                .eq(JkRegion::getRegionCode, role.getRegionCode())
                .eq(JkRegion::getIsDeleted, false)
                .last("limit 1"));

        List<JkAgentRelationResponse> children = relationService.list(null, memberUserId, true);
        Set<Long> allTeam = collectAllTeamUserIds(memberUserId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", memberUserId);
        data.put("userName", StrUtil.blankToDefault(user.getRealName(), user.getNickname()));
        data.put("nickname", user.getNickname());
        data.put("avatar", user.getAvatar());
        data.put("userPhone", user.getPhone());
        data.put("parentUserId", currentUserId);
        data.put("parentName", relation.getParentName());
        data.put("relationId", relation.getId());
        data.put("relationType", relation.getRelationType());
        data.put("bindSource", relation.getBindSource());
        data.put("joinTime", relation.getEffectiveTime());
        data.put("createTime", relation.getCreateTime());
        data.put("roleCode", role == null ? null : role.getRoleCode());
        data.put("roleName", roleDefinition == null ? null : roleDefinition.getRoleName());
        data.put("regionCode", role == null ? null : role.getRegionCode());
        data.put("regionName", region == null ? null : region.getRegionName());
        data.put("directTeamCount", children.size());
        data.put("directCount", children.size());
        data.put("teamCount", allTeam.size());
        data.put("statusText", roleStatusText(role));
        data.put("freezeReason", role == null ? null : role.getFreezeReason());
        data.put("contributionAvailable", false);
        data.put("contributionMessage", "现有佣金记录未保存来源成员快照，暂不能准确计算成员贡献");
        data.put("contributionRecords", Collections.emptyList());
        data.put("activityList", realActivities(relation, role));
        return data;
    }

    private List<Map<String, Object>> realActivities(JkAgentRelationResponse relation, JkUserBusinessRole role) {
        List<Map<String, Object>> activities = new ArrayList<>();
        if (relation.getEffectiveTime() != null) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", "RELATION_" + relation.getId());
            item.put("title", "加入直属团队");
            item.put("createTime", relation.getEffectiveTime());
            item.put("sourceType", "AGENT_RELATION");
            activities.add(item);
        }
        if (role != null && role.getEffectiveTime() != null) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", "ROLE_" + role.getId());
            item.put("title", "业务身份生效");
            item.put("createTime", role.getEffectiveTime());
            item.put("sourceType", "BUSINESS_ROLE");
            activities.add(item);
        }
        activities.sort((a, b) -> {
            Date left = (Date) a.get("createTime");
            Date right = (Date) b.get("createTime");
            return right.compareTo(left);
        });
        return activities;
    }

    private String roleStatusText(JkUserBusinessRole role) {
        if (role == null) return "无生效身份";
        if (Boolean.TRUE.equals(role.getFreezeStatus())) return "已冻结";
        if (Boolean.FALSE.equals(role.getStatus())) return "已停用";
        return "正常";
    }

    private void enrichRelationMember(JkAgentRelationResponse relation) {
        if (relation == null || relation.getUserId() == null) return;
        JkUserBusinessRole role = userRoleDao.selectOne(new LambdaQueryWrapper<JkUserBusinessRole>()
                .eq(JkUserBusinessRole::getUserId, relation.getUserId())
                .eq(JkUserBusinessRole::getAuditStatus, JkBizConstants.AUDIT_STATUS_EFFECTIVE)
                .eq(JkUserBusinessRole::getIsDeleted, false)
                .orderByDesc(JkUserBusinessRole::getIsPrimary)
                .orderByDesc(JkUserBusinessRole::getId)
                .last("limit 1"));
        if (role == null) return;
        relation.setRoleCode(role.getRoleCode());
        relation.setRegionCode(role.getRegionCode());
        relation.setStatusText(roleStatusText(role));
        JkBusinessRole definition = roleDao.selectOne(new LambdaQueryWrapper<JkBusinessRole>()
                .eq(JkBusinessRole::getRoleCode, role.getRoleCode())
                .eq(JkBusinessRole::getIsDeleted, false)
                .last("limit 1"));
        if (definition != null) relation.setRoleName(definition.getRoleName());
        if (StrUtil.isNotBlank(role.getRegionCode())) {
            JkRegion region = regionDao.selectOne(new LambdaQueryWrapper<JkRegion>()
                    .eq(JkRegion::getRegionCode, role.getRegionCode())
                    .eq(JkRegion::getIsDeleted, false)
                    .last("limit 1"));
            if (region != null) relation.setRegionName(region.getRegionName());
        }
        User user = userService.getById(relation.getUserId().intValue());
        if (user != null) relation.setAvatar(user.getAvatar());
        relation.setDirectTeamCount(relationService.list(null, relation.getUserId(), true).size());
        relation.setTeamCount(collectAllTeamUserIds(relation.getUserId()).size());
    }

    private Set<Long> collectAllTeamUserIds(Long rootUserId) {
        Set<Long> result = new HashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(rootUserId);
        int visitedParents = 0;
        while (!queue.isEmpty() && visitedParents < 1000) {
            Long parentId = queue.removeFirst();
            visitedParents++;
            List<JkAgentRelationResponse> children = relationService.list(null, parentId, true);
            for (JkAgentRelationResponse child : children) {
                if (child.getUserId() != null && result.add(child.getUserId())) queue.addLast(child.getUserId());
            }
        }
        return result;
    }

    @Override
    public JkPromotionQrcodeResponse promotionQrcode(Long userId) {
        String path = "/pages/index/index?spread=" + userId;
        String siteUrl = systemConfigService.getValueByKey(SysConfigConstants.CONFIG_KEY_SITE_URL);
        String shareUrl = StrUtil.isBlank(siteUrl) ? path : siteUrl.replaceAll("/+$", "") + path;
        JkPromotionQrcodeResponse response = new JkPromotionQrcodeResponse();
        response.setUserId(userId);
        response.setSharePath(path);
        response.setShareUrl(shareUrl);
        response.setDescription("扫码或分享后仍走 CRMEB 原 spread 绑定入口，并同步写入九州康上下级关系。首次有效关系默认保持，换绑需提交审核。");
        try {
            response.setQrCodeBase64(QRCodeUtil.crateQRCode(shareUrl, 480, 480));
        } catch (Exception e) {
            throw new IllegalStateException("推广二维码生成失败", e);
        }
        return response;
    }

    @Override
    public List<JkOptionResponse> eligibleParentOptions(Long userId, String keyword, int limit) {
        if (StrUtil.isBlank(keyword)) return Collections.emptyList();
        int safeLimit = Math.max(1, Math.min(limit, 20));
        JkUserContext current = contextService.getFrontContext(userId);
        LambdaQueryWrapper<JkUserBusinessRole> roleQuery = new LambdaQueryWrapper<JkUserBusinessRole>()
                .in(JkUserBusinessRole::getRoleCode, Arrays.asList(JkBizConstants.ROLE_MAKER, JkBizConstants.ROLE_PARTNER, JkBizConstants.ROLE_COUNTY_AGENT))
                .eq(JkUserBusinessRole::getAuditStatus, JkBizConstants.AUDIT_STATUS_EFFECTIVE)
                .eq(JkUserBusinessRole::getFreezeStatus, false)
                .eq(JkUserBusinessRole::getIsDeleted, false)
                .ne(JkUserBusinessRole::getUserId, userId)
                .orderByDesc(JkUserBusinessRole::getIsPrimary);
        if (current != null && StrUtil.isNotBlank(current.getRegionCode())) roleQuery.eq(JkUserBusinessRole::getRegionCode, current.getRegionCode());
        List<JkUserBusinessRole> roles = userRoleDao.selectList(roleQuery);
        if (roles.isEmpty()) return Collections.emptyList();
        Map<Long, JkUserBusinessRole> primary = new LinkedHashMap<>();
        for (JkUserBusinessRole role : roles) primary.putIfAbsent(role.getUserId(), role);
        String key = keyword.trim();
        LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<User>().eq(User::getStatus, true)
                .in(User::getUid, primary.keySet().stream().map(Long::intValue).collect(java.util.stream.Collectors.toList()));
        if (key.matches("\\d+")) {
            userQuery.and(q -> q.like(User::getNickname, key).or().like(User::getRealName, key)
                    .or().like(User::getPhone, key).or().eq(User::getUid, Integer.valueOf(key)));
        } else {
            userQuery.and(q -> q.like(User::getNickname, key).or().like(User::getRealName, key).or().like(User::getPhone, key));
        }
        List<User> users = userService.list(userQuery.orderByDesc(User::getUid).last("limit " + safeLimit));
        Map<String, String> roleNames = roleDao.selectList(new LambdaQueryWrapper<JkBusinessRole>()
                        .eq(JkBusinessRole::getIsDeleted, false)).stream()
                .collect(java.util.stream.Collectors.toMap(JkBusinessRole::getRoleCode, JkBusinessRole::getRoleName, (a, b) -> a));
        List<JkOptionResponse> result = new ArrayList<>();
        for (User user : users) {
            JkUserBusinessRole role = primary.get(Long.valueOf(user.getUid()));
            String roleName = roleNames.getOrDefault(role.getRoleCode(), role.getRoleCode());
            String name = StrUtil.blankToDefault(user.getRealName(), user.getNickname());
            result.add(new JkOptionResponse().setValue(String.valueOf(user.getUid()))
                    .setLabel(name + " / " + maskPhone(user.getPhone()) + " / " + roleName)
                    .setPhone(maskPhone(user.getPhone())).setRoleCode(role.getRoleCode()).setRoleName(roleName)
                    .setRegionCode(role.getRegionCode()).setDisabled(false));
        }
        return result;
    }

    private Date startOfToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date startOfMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private String maskPhone(String phone) {
        if (StrUtil.isBlank(phone) || phone.length() < 7) return StrUtil.blankToDefault(phone, "无手机号");
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
