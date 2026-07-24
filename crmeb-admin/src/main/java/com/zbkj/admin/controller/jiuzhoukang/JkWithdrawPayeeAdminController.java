package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.constants.jiuzhoukang.JkPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkWithdrawApply;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.dao.jiuzhoukang.JkWithdrawApplyDao;
import com.zbkj.service.service.jiuzhoukang.commission.JkWithdrawPayeeAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * 提现打款收款信息查看接口。
 *
 * <p>完整银行卡号仅允许具备“确认打款”权限的管理员按单次提现申请主动查看。
 * 使用 POST 且显式禁止浏览器和代理缓存；普通提现列表和前台接口只返回掩码。</p>
 */
@RestController
@RequestMapping("api/admin/jk/withdraw")
public class JkWithdrawPayeeAdminController {
    @Autowired private JkWithdrawApplyDao withdrawApplyDao;
    @Autowired private JkWithdrawPayeeAccountService payeeAccountService;

    @PostMapping("/payee/{id}/reveal")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_WITHDRAW_CONFIRM_PAID + "')")
    public CommonResult<Map<String, Object>> revealPayee(@PathVariable Long id, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0L);
        JkWithdrawApply apply = withdrawApplyDao.selectById(id);
        if (apply == null || Boolean.TRUE.equals(apply.getIsDeleted())) {
            throw new IllegalArgumentException("提现申请不存在");
        }
        return CommonResult.success(payeeAccountService.revealSnapshot(apply.getPayeeSnapshotJson()));
    }
}
