package com.zbkj.service.service.impl.jiuzhoukang.risk;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkRiskEvent;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkRiskHandleRequest;
import com.zbkj.service.dao.jiuzhoukang.JkRiskEventDao;
import com.zbkj.service.service.jiuzhoukang.risk.JkRiskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** 第六阶段风险事件实现。当前不自动改账，避免风控规则误判直接破坏业务数据。 */
@Service
public class JkRiskServiceImpl implements JkRiskService {
    @Autowired private JkRiskEventDao dao;

    @Override @Transactional(rollbackFor=Exception.class)
    public JkRiskEvent record(String type,String level,String sourceType,Long sourceId,String sourceNo,Long userId,String summary,String detail){
        return insert(null,type,level,sourceType,sourceId,sourceNo,userId,summary,detail);
    }

    @Override @Transactional(rollbackFor=Exception.class)
    public JkRiskEvent recordOnce(String key,String type,String level,String sourceType,Long sourceId,String sourceNo,Long userId,String summary,String detail){
        if(StrUtil.isBlank(key)) return record(type,level,sourceType,sourceId,sourceNo,userId,summary,detail);
        JkRiskEvent old=dao.selectOne(new LambdaQueryWrapper<JkRiskEvent>().eq(JkRiskEvent::getIdempotencyKey,key).eq(JkRiskEvent::getIsDeleted,false).last("limit 1"));
        if(old!=null)return old;
        try{return insert(key,type,level,sourceType,sourceId,sourceNo,userId,summary,detail);}catch(DuplicateKeyException ex){return dao.selectOne(new LambdaQueryWrapper<JkRiskEvent>().eq(JkRiskEvent::getIdempotencyKey,key).last("limit 1"));}
    }

    private JkRiskEvent insert(String key,String type,String level,String sourceType,Long sourceId,String sourceNo,Long userId,String summary,String detail){
        Date now=new Date();
        JkRiskEvent e=new JkRiskEvent().setEventNo("RISK"+IdWorker.getIdStr()).setIdempotencyKey(key).setRiskType(type).setRiskLevel(level)
                .setSourceType(sourceType).setSourceId(sourceId).setSourceNo(sourceNo).setUserId(userId).setSummary(summary)
                .setDetailJson(detail).setStatus("OPEN").setIsDeleted(false).setCreateTime(now).setUpdateTime(now).setVersion(0);
        dao.insert(e);return e;
    }

    @Override public PageInfo<JkRiskEvent> list(String type,String level,String status,PageParamRequest p){
        Page<JkRiskEvent> page=PageHelper.startPage(p.getPage(),p.getLimit());
        LambdaQueryWrapper<JkRiskEvent> q=new LambdaQueryWrapper<JkRiskEvent>().eq(JkRiskEvent::getIsDeleted,false).orderByDesc(JkRiskEvent::getId);
        if(StrUtil.isNotBlank(type))q.eq(JkRiskEvent::getRiskType,type);if(StrUtil.isNotBlank(level))q.eq(JkRiskEvent::getRiskLevel,level);if(StrUtil.isNotBlank(status))q.eq(JkRiskEvent::getStatus,status);
        return CommonPage.copyPageInfo(page,dao.selectList(q));
    }

    @Override @Transactional(rollbackFor=Exception.class)
    public JkRiskEvent handle(Long admin,JkRiskHandleRequest r){
        String target="CLOSE".equalsIgnoreCase(r.getAction())?"CLOSED":"HANDLED";
        int n=dao.update(null,new UpdateWrapper<JkRiskEvent>().eq("id",r.getId()).in("status",Arrays.asList("OPEN","HANDLED")).set("status",target)
                .set("handle_user_id",admin).set("handle_time",new Date()).set("handle_remark",r.getRemark()).set("update_time",new Date()));
        if(n!=1)throw new CrmebException("风险事件不存在或已关闭");return dao.selectById(r.getId());
    }
}
