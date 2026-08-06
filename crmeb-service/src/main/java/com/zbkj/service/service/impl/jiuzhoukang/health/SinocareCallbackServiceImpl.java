package com.zbkj.service.service.impl.jiuzhoukang.health;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.jiuzhoukang.JkSinocareCallbackLog;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.SinocareEnvelopeRequest;
import com.zbkj.common.response.jiuzhoukang.JkSinocareCallbackLogResponse;
import com.zbkj.service.dao.jiuzhoukang.JkSinocareCallbackLogDao;
import com.zbkj.service.service.jiuzhoukang.health.SinocareCallbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import cn.hutool.core.util.StrUtil;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SinocareCallbackServiceImpl implements SinocareCallbackService {
    @Autowired private JkSinocareCallbackLogDao callbackLogDao;
    @Autowired private JkHealthSensitiveCodec codec;
    @Autowired private SinocareCallbackProcessor processor;

    @Override
    public void receive(String eventType, SinocareEnvelopeRequest envelope) {
        Date now = new Date();
        JkSinocareCallbackLog log = new JkSinocareCallbackLog()
                .setEventType(eventType).setEventId("RECEIVED-" + IdWorker.getIdStr())
                .setPayloadCipher(codec.encode(envelope.getCiphertext())).setSignature(envelope.getSignature())
                .setProcessStatus("RECEIVED").setRetryCount(0).setCreateTime(now).setUpdateTime(now);
        callbackLogDao.insert(log);
        processor.process(log.getId());
    }

    @Override
    public PageInfo<JkSinocareCallbackLogResponse> list(String eventType, String processStatus, String uniqueId, PageParamRequest page) {
        Page<JkSinocareCallbackLog> resultPage = PageHelper.startPage(page.getPage(), page.getLimit());
        LambdaQueryWrapper<JkSinocareCallbackLog> query = new LambdaQueryWrapper<JkSinocareCallbackLog>()
                .orderByDesc(JkSinocareCallbackLog::getId);
        if (StrUtil.isNotBlank(eventType)) query.eq(JkSinocareCallbackLog::getEventType, eventType);
        if (StrUtil.isNotBlank(processStatus)) query.eq(JkSinocareCallbackLog::getProcessStatus, processStatus);
        if (StrUtil.isNotBlank(uniqueId)) query.eq(JkSinocareCallbackLog::getUniqueId, uniqueId);
        List<JkSinocareCallbackLogResponse> rows = callbackLogDao.selectList(query).stream()
                .map(JkSinocareCallbackLogResponse::from).collect(Collectors.toList());
        return CommonPage.copyPageInfo(resultPage, rows);
    }

    @Override
    public JkSinocareCallbackLogResponse retry(Long id) {
        JkSinocareCallbackLog log = callbackLogDao.selectById(id);
        if (log == null || !"FAILED".equals(log.getProcessStatus())) {
            throw new IllegalStateException("仅处理失败的三诺回调允许重试");
        }
        log.setProcessStatus("RECEIVED").setErrorMessage(null)
                .setRetryCount((log.getRetryCount() == null ? 0 : log.getRetryCount()) + 1)
                .setUpdateTime(new Date());
        callbackLogDao.updateById(log);
        processor.process(log.getId());
        return JkSinocareCallbackLogResponse.from(log);
    }
}
