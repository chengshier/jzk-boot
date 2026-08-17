package com.zbkj.service.service.impl.jiuzhoukang.health;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.model.jiuzhoukang.*;
import com.zbkj.service.dao.jiuzhoukang.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import java.util.Date;
@Component public class SinocareCallbackProcessor {
 @Autowired private JkSinocareCallbackLogDao logs; @Autowired private JkSinocareAuthorizationDao authorizations;
 @Autowired private JkHealthSensitiveCodec secure; @Autowired private SinocareRsaCodec rsa;
 @Autowired private SinocarePayloadStructureLogger payloadStructureLogger;
 @Autowired private JkSinocareDeviceSessionDao sessions;
 @Autowired private JkHealthDataDao healthData; @Autowired private JkSinocareReportDao reports;
 @Async public void process(Long id){ JkSinocareCallbackLog log=logs.selectById(id); if(log==null)return; try{
  log.setProcessStatus("PROCESSING").setUpdateTime(new Date());logs.updateById(log);
  JSONObject body=JSON.parseObject(rsa.verifyAndDecrypt(secure.decode(log.getPayloadCipher()),log.getSignature()));
  payloadStructureLogger.log(log.getEventType(), body);
  String uniqueId=body.getString("uniqueId"), eventId=body.getString("id");
  if(eventId==null) eventId=body.getString("deviceSn")+":"+body.getString("detectionDate");
  log.setUniqueId(uniqueId).setEventId(eventId);
  JkSinocareAuthorization auth=authorizations.selectOne(new LambdaQueryWrapper<JkSinocareAuthorization>().eq(JkSinocareAuthorization::getUniqueId,uniqueId).last("limit 1"));
  if(auth==null){log.setProcessStatus("UNMATCHED").setErrorMessage("未找到本平台签发的 uniqueId");}
  else {applyAuthorization(log, body, auth); applyDevice(log, body); applyCgm(log, body, auth); applyReport(log, body); log.setProcessStatus("SUCCESS").setErrorMessage(null);}
 }catch(Exception e){log.setProcessStatus("FAILED").setErrorMessage(e.getMessage()==null?"三诺回调处理失败":e.getMessage().substring(0,Math.min(500,e.getMessage().length())));}finally{log.setUpdateTime(new Date());logs.updateById(log);} }
 private void applyAuthorization(JkSinocareCallbackLog log, JSONObject body, JkSinocareAuthorization auth){
  if(!"1001".equals(log.getEventType())) return;
  Integer status=body.getInteger("status"); Date now=new Date();
  if(Integer.valueOf(1).equals(status)) auth.setStatus("AUTHORIZED").setAuthorizedAt(new Date(body.getLongValue("bindTime"))).setSourceEventId(log.getEventId());
  else if(Integer.valueOf(0).equals(status)) auth.setStatus("REVOKED").setRevokedAt(new Date(body.getLongValue("unbindTime"))).setSourceEventId(log.getEventId());
  else throw new IllegalArgumentException("三诺授权状态非法");
  auth.setUpdateTime(now); authorizations.updateById(auth);
 }
 private void applyDevice(JkSinocareCallbackLog log, JSONObject body){ if(!"1002".equals(log.getEventType()))return; String sn=body.getString("deviceSn"); if(sn==null)throw new IllegalArgumentException("设备回调缺少 deviceSn"); JkSinocareDeviceSession row=sessions.selectOne(new LambdaQueryWrapper<JkSinocareDeviceSession>().eq(JkSinocareDeviceSession::getUniqueId,log.getUniqueId()).eq(JkSinocareDeviceSession::getDeviceSn,sn).last("limit 1")); Date now=new Date(); if(row==null)row=new JkSinocareDeviceSession().setUniqueId(log.getUniqueId()).setDeviceSn(sn).setCreateTime(now); row.setStatus(body.getInteger("status")).setProductName(body.getString("productName")).setDetectionStartTime(new Date(body.getLongValue("detectionStartTime"))).setDetectionEndTime(new Date(body.getLongValue("detectionEndTime"))).setUpdateTime(now); if(row.getId()==null)sessions.insert(row);else sessions.updateById(row); }
 private void applyCgm(JkSinocareCallbackLog log, JSONObject body, JkSinocareAuthorization auth){ if(!"1003".equals(log.getEventType()))return; for(Object raw:body.getJSONArray("data")){JSONObject p=(JSONObject)raw; String key="SINOCARE:"+body.getString("deviceSn")+":"+body.getInteger("detectionDate")+":"+p.getInteger("sn"); if(healthData.selectCount(new LambdaQueryWrapper<JkHealthData>().eq(JkHealthData::getExternalNo,key))>0)continue; Date now=new Date(); healthData.insert(new JkHealthData().setExternalNo(key).setUserId(auth.getUserId()).setDataType("GLUCOSE").setNumericValue(p.getBigDecimal("value")).setUnit("mmol/L").setMeasuredAt(new Date(p.getLongValue("time"))).setSourceType("SINOCARE").setRiskLevel("NORMAL").setStatus("VALID").setIsDeleted(false).setCreateUserId(auth.getUserId()).setUpdateUserId(auth.getUserId()).setCreateTime(now).setUpdateTime(now).setVersion(0)); } }
 private void applyReport(JkSinocareCallbackLog log, JSONObject body){if(!"1004".equals(log.getEventType())&&!"1005".equals(log.getEventType()))return; if(reports.selectCount(new LambdaQueryWrapper<JkSinocareReport>().eq(JkSinocareReport::getReportType,"1004".equals(log.getEventType())?"DIGITAL":"PDF").eq(JkSinocareReport::getEventId,log.getEventId()))>0)return; Date now=new Date();reports.insert(new JkSinocareReport().setEventId(log.getEventId()).setUniqueId(log.getUniqueId()).setDeviceSn(body.getString("deviceSn")).setReportType("1004".equals(log.getEventType())?"DIGITAL":"PDF").setPayloadCipher(secure.encode(body.toJSONString())).setCreateTime(now).setUpdateTime(now));}
}
