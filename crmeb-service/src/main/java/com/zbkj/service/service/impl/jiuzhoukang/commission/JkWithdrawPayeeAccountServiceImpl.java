package com.zbkj.service.service.impl.jiuzhoukang.commission;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zbkj.common.model.jiuzhoukang.JkWithdrawPayeeAccount;
import com.zbkj.common.request.jiuzhoukang.JkWithdrawPayeeAccountSaveRequest;
import com.zbkj.common.response.jiuzhoukang.JkWithdrawPayeeAccountResponse;
import com.zbkj.service.dao.jiuzhoukang.JkWithdrawPayeeAccountDao;
import com.zbkj.service.service.jiuzhoukang.commission.JkWithdrawPayeeAccountService;
import com.zbkj.service.service.jiuzhoukang.support.JkSensitiveFieldCryptoSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 提现收款账户服务：完整卡号只在服务端认证加密保存，列表仅返回掩码。 */
@Service
public class JkWithdrawPayeeAccountServiceImpl implements JkWithdrawPayeeAccountService {
    private static final String ACCOUNT_TYPE_BANK = "BANK";

    @Autowired private JkWithdrawPayeeAccountDao accountDao;
    @Autowired private JkSensitiveFieldCryptoSupport cryptoSupport;

    @Override
    public List<JkWithdrawPayeeAccountResponse> list(Long userId) {
        if (userId == null) return Collections.emptyList();
        List<JkWithdrawPayeeAccount> rows = accountDao.selectList(new LambdaQueryWrapper<JkWithdrawPayeeAccount>()
                .eq(JkWithdrawPayeeAccount::getUserId, userId)
                .eq(JkWithdrawPayeeAccount::getStatus, true)
                .eq(JkWithdrawPayeeAccount::getIsDeleted, false)
                .orderByDesc(JkWithdrawPayeeAccount::getIsDefault)
                .orderByDesc(JkWithdrawPayeeAccount::getId));
        List<JkWithdrawPayeeAccountResponse> result = new ArrayList<>();
        for (JkWithdrawPayeeAccount row : rows) result.add(toResponse(row));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkWithdrawPayeeAccountResponse save(Long userId, JkWithdrawPayeeAccountSaveRequest request) {
        if (userId == null) throw new IllegalArgumentException("请先登录");
        JkWithdrawPayeeAccount entity = request != null && request.getId() != null
                ? requireOwned(userId, request.getId(), false) : null;
        validate(request, entity == null);

        String bankAccount = normalize(request.getBankAccount());
        boolean cardChanged = StrUtil.isNotBlank(bankAccount);
        if (cardChanged) assertNoDuplicate(userId, request.getId(), bankAccount);

        Date now = new Date();
        if (entity == null) {
            entity = new JkWithdrawPayeeAccount().setUserId(userId).setIsDeleted(false).setStatus(true)
                    .setVersion(0).setCreateTime(now);
        }
        Integer count = accountDao.selectCount(new LambdaQueryWrapper<JkWithdrawPayeeAccount>()
                .eq(JkWithdrawPayeeAccount::getUserId, userId)
                .eq(JkWithdrawPayeeAccount::getStatus, true)
                .eq(JkWithdrawPayeeAccount::getIsDeleted, false)
                .ne(entity.getId() != null, JkWithdrawPayeeAccount::getId, entity.getId()));
        boolean firstAccount = count == null || count == 0;
        boolean makeDefault = firstAccount || Boolean.TRUE.equals(request.getSetDefault()) || Boolean.TRUE.equals(entity.getIsDefault());
        if (makeDefault) clearDefault(userId);

        entity.setAccountType(ACCOUNT_TYPE_BANK)
                .setAccountName(request.getAccountName().trim())
                .setBankName(request.getBankName().trim())
                .setIsDefault(makeDefault)
                .setStatus(true)
                .setUpdateTime(now);
        if (cardChanged) {
            entity.setBankAccountCipher(cryptoSupport.encrypt(bankAccount))
                    .setBankAccountHash(cryptoSupport.sha256(bankAccount))
                    .setBankAccountMask(mask(bankAccount));
        }
        if (entity.getId() == null) accountDao.insert(entity); else accountDao.updateById(entity);
        return toResponse(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkWithdrawPayeeAccountResponse setDefault(Long userId, Long id) {
        JkWithdrawPayeeAccount entity = requireOwned(userId, id, true);
        clearDefault(userId);
        entity.setIsDefault(true).setUpdateTime(new Date());
        accountDao.updateById(entity);
        return toResponse(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long userId, Long id) {
        JkWithdrawPayeeAccount entity = requireOwned(userId, id, false);
        boolean wasDefault = Boolean.TRUE.equals(entity.getIsDefault());
        entity.setIsDeleted(true).setStatus(false).setIsDefault(false).setUpdateTime(new Date());
        accountDao.updateById(entity);
        if (wasDefault) promoteNewestAsDefault(userId);
    }

    @Override
    public String buildSnapshotJson(Long userId, Long id) {
        JkWithdrawPayeeAccount entity = requireOwned(userId, id, true);
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("payeeAccountId", entity.getId());
        snapshot.put("accountType", entity.getAccountType());
        snapshot.put("accountName", entity.getAccountName());
        snapshot.put("bankName", entity.getBankName());
        snapshot.put("bankAccountCipher", entity.getBankAccountCipher());
        snapshot.put("bankAccountMask", entity.getBankAccountMask());
        snapshot.put("snapshotTime", new Date());
        return JSONUtil.toJsonStr(snapshot);
    }

    @Override
    public Map<String, Object> maskedSnapshot(String snapshotJson) {
        if (StrUtil.isBlank(snapshotJson)) return Collections.emptyMap();
        JSONObject source;
        try {
            source = JSONUtil.parseObj(snapshotJson);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("payeeAccountId", source.getLong("payeeAccountId"));
        result.put("accountType", source.getStr("accountType"));
        result.put("accountName", source.getStr("accountName"));
        result.put("bankName", source.getStr("bankName"));
        result.put("bankAccountMask", source.getStr("bankAccountMask"));
        return result;
    }

    @Override
    public Map<String, Object> revealSnapshot(String snapshotJson) {
        if (StrUtil.isBlank(snapshotJson)) throw new IllegalArgumentException("提现申请没有收款账户快照");
        JSONObject source = JSONUtil.parseObj(snapshotJson);
        String cipher = source.getStr("bankAccountCipher");
        if (StrUtil.isBlank(cipher)) throw new IllegalArgumentException("历史提现申请未保存可解密的银行卡快照");
        Map<String, Object> result = new LinkedHashMap<>(maskedSnapshot(snapshotJson));
        result.put("bankAccount", cryptoSupport.decrypt(cipher));
        return result;
    }

    private void validate(JkWithdrawPayeeAccountSaveRequest request, boolean cardRequired) {
        if (request == null) throw new IllegalArgumentException("收款账户不能为空");
        String accountType = StrUtil.blankToDefault(request.getAccountType(), ACCOUNT_TYPE_BANK).trim().toUpperCase();
        if (!ACCOUNT_TYPE_BANK.equals(accountType)) throw new IllegalArgumentException("当前仅支持银行卡提现");
        if (StrUtil.isBlank(request.getAccountName())) throw new IllegalArgumentException("请填写收款人姓名");
        if (StrUtil.isBlank(request.getBankName())) throw new IllegalArgumentException("请填写开户银行");
        String bankAccount = normalize(request.getBankAccount());
        if (cardRequired && StrUtil.isBlank(bankAccount)) throw new IllegalArgumentException("请填写银行卡号");
        if (StrUtil.isNotBlank(bankAccount) && !bankAccount.matches("\\d{8,30}")) {
            throw new IllegalArgumentException("请填写有效银行卡号");
        }
    }

    private void assertNoDuplicate(Long userId, Long excludeId, String bankAccount) {
        String accountHash = cryptoSupport.sha256(bankAccount);
        JkWithdrawPayeeAccount duplicate = accountDao.selectOne(new LambdaQueryWrapper<JkWithdrawPayeeAccount>()
                .eq(JkWithdrawPayeeAccount::getUserId, userId)
                .eq(JkWithdrawPayeeAccount::getBankAccountHash, accountHash)
                .eq(JkWithdrawPayeeAccount::getIsDeleted, false)
                .ne(excludeId != null, JkWithdrawPayeeAccount::getId, excludeId)
                .last("limit 1"));
        if (duplicate != null) throw new IllegalArgumentException("该银行卡已添加，请勿重复保存");
    }

    private JkWithdrawPayeeAccount requireOwned(Long userId, Long id, boolean requireEnabled) {
        if (id == null) throw new IllegalArgumentException("请选择收款账户");
        JkWithdrawPayeeAccount entity = accountDao.selectById(id);
        if (entity == null || Boolean.TRUE.equals(entity.getIsDeleted()) || !userId.equals(entity.getUserId())) {
            throw new IllegalArgumentException("收款账户不存在");
        }
        if (requireEnabled && !Boolean.TRUE.equals(entity.getStatus())) throw new IllegalArgumentException("收款账户已停用");
        return entity;
    }

    private void clearDefault(Long userId) {
        accountDao.update(null, new LambdaUpdateWrapper<JkWithdrawPayeeAccount>()
                .eq(JkWithdrawPayeeAccount::getUserId, userId)
                .eq(JkWithdrawPayeeAccount::getIsDeleted, false)
                .set(JkWithdrawPayeeAccount::getIsDefault, false)
                .set(JkWithdrawPayeeAccount::getUpdateTime, new Date()));
    }

    private void promoteNewestAsDefault(Long userId) {
        JkWithdrawPayeeAccount replacement = accountDao.selectOne(new LambdaQueryWrapper<JkWithdrawPayeeAccount>()
                .eq(JkWithdrawPayeeAccount::getUserId, userId)
                .eq(JkWithdrawPayeeAccount::getStatus, true)
                .eq(JkWithdrawPayeeAccount::getIsDeleted, false)
                .orderByDesc(JkWithdrawPayeeAccount::getId)
                .last("limit 1"));
        if (replacement != null) {
            replacement.setIsDefault(true).setUpdateTime(new Date());
            accountDao.updateById(replacement);
        }
    }

    private JkWithdrawPayeeAccountResponse toResponse(JkWithdrawPayeeAccount entity) {
        return new JkWithdrawPayeeAccountResponse().setId(entity.getId()).setAccountType(entity.getAccountType())
                .setAccountName(entity.getAccountName()).setBankName(entity.getBankName())
                .setBankAccountMask(entity.getBankAccountMask()).setIsDefault(entity.getIsDefault())
                .setStatus(entity.getStatus()).setCreateTime(entity.getCreateTime()).setUpdateTime(entity.getUpdateTime());
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("[\\s-]", "");
    }

    private String mask(String value) {
        String account = normalize(value);
        if (account.length() <= 8) return account;
        return account.substring(0, 4) + " **** **** " + account.substring(account.length() - 4);
    }
}
