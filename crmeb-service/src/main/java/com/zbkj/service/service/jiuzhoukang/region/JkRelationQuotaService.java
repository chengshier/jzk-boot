package com.zbkj.service.service.jiuzhoukang.region;

import com.zbkj.common.model.jiuzhoukang.JkRelationLimitRule;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkRelationLimitRuleSaveRequest;
import com.zbkj.common.response.jiuzhoukang.JkRelationQuotaResponse;
import com.github.pagehelper.PageInfo;

/** 直属下级人数规则、并发占用和换绑预占服务。 */
public interface JkRelationQuotaService {
    JkRelationQuotaResponse quota(Long parentUserId, Long childUserId);

    /** 首次绑定或管理员强制调整时，在当前事务中占用一个名额。 */
    void occupy(Long parentUserId, Long childUserId, Long operatorId);

    /** 换绑申请提交时预占目标上级名额。 */
    void reserve(String requestNo, Long parentUserId, Long childUserId, Long operatorId);

    /** 审核通过后消费预占；旧数据没有预占时会退化为实时占用。 */
    void consume(String requestNo, Long parentUserId, Long childUserId, Long operatorId);

    /** 驳回、取消或过期时释放预占。 */
    void releaseReservation(String requestNo, String finalStatus, Long operatorId);

    /** 关系失效后同步旧上级额度快照。 */
    void syncUsage(Long parentUserId, Long operatorId);

    PageInfo<JkRelationLimitRule> listRules(String keyword, Boolean status, PageParamRequest pageParam);
    JkRelationLimitRule saveRule(JkRelationLimitRuleSaveRequest request, Long operatorId);
    JkRelationLimitRule updateRuleStatus(Long id, Boolean status, Long operatorId);
}
