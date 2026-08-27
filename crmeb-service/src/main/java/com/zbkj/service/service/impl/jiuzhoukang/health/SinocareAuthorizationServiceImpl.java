package com.zbkj.service.service.impl.jiuzhoukang.health;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.model.jiuzhoukang.JkSinocareAuthorization;
import com.zbkj.common.response.jiuzhoukang.JkSinocareAuthorizationPrepareResponse;
import com.zbkj.service.dao.jiuzhoukang.JkSinocareAuthorizationDao;
import com.zbkj.service.service.jiuzhoukang.health.SinocareAuthorizationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.UUID;
@Service public class SinocareAuthorizationServiceImpl implements SinocareAuthorizationService {
 @Autowired private JkSinocareAuthorizationDao dao;
 @Value("${jk.health.sinocare.app-id:}") private String appId;
 @Value("${jk.health.sinocare.authorization-h5-url:}") private String authorizationH5Url;
 @Value("${jk.health.sinocare.redirect-url:}") private String redirectUrl;
 public JkSinocareAuthorization issueForUser(Long userId){
  JkSinocareAuthorization old=dao.selectOne(new LambdaQueryWrapper<JkSinocareAuthorization>().eq(JkSinocareAuthorization::getUserId,userId).last("limit 1"));
  if(old!=null)return old;
  Date now=new Date(); JkSinocareAuthorization row=new JkSinocareAuthorization().setUserId(userId)
   // 三诺限制 uniqueId 为 32 位以内，UUID 去掉连字符后正好为 32 位。
   .setUniqueId(UUID.randomUUID().toString().replace("-", "")).setStatus("PENDING")
   .setCreateTime(now).setUpdateTime(now); dao.insert(row); return row;
 }

 @Override public JkSinocareAuthorizationPrepareResponse buildAuthorizationUrl(Long userId){
  if(isBlank(appId) || isBlank(authorizationH5Url) || isBlank(redirectUrl)) throw new IllegalStateException("三诺授权配置未完成");
  if(isBlank(redirectUrl)) throw new IllegalArgumentException("redirectUrl不能为空");
  JkSinocareAuthorization authorization=issueForUser(userId);
  return new JkSinocareAuthorizationPrepareResponse().setUniqueId(authorization.getUniqueId())
   .setAuthorizationUrl(authorizationH5Url + (authorizationH5Url.contains("?") ? "&" : "?")
    + "appId=" + encode(appId) + "&uniqueId=" + encode(authorization.getUniqueId()) + "&redirectUrl=" + encode(redirectUrl));
 }

 private String encode(String value){ try{return URLEncoder.encode(value,"UTF-8");}catch(UnsupportedEncodingException e){throw new IllegalStateException(e);} }
 private boolean isBlank(String value){return value==null||value.trim().isEmpty();}
}
