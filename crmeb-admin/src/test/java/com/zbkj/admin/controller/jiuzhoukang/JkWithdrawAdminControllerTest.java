package com.zbkj.admin.controller.jiuzhoukang;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.zbkj.common.model.jiuzhoukang.JkCommissionAccount;
import com.zbkj.common.model.jiuzhoukang.JkCommissionFlow;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRecord;
import com.zbkj.common.model.jiuzhoukang.JkCommissionReverse;
import com.zbkj.common.model.jiuzhoukang.JkFundAccount;
import com.zbkj.common.model.jiuzhoukang.JkFundFlow;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionAccountDao;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionFlowDao;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionRecordDao;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionReverseDao;
import com.zbkj.service.dao.jiuzhoukang.JkFundAccountDao;
import com.zbkj.service.dao.jiuzhoukang.JkFundFlowDao;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContextService;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JkWithdrawAdminControllerTest {

    @BeforeClass
    public static void initializeMybatisMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, JkCommissionRecord.class);
        TableInfoHelper.initTableInfo(assistant, JkCommissionFlow.class);
        TableInfoHelper.initTableInfo(assistant, JkCommissionReverse.class);
        TableInfoHelper.initTableInfo(assistant, JkFundAccount.class);
        TableInfoHelper.initTableInfo(assistant, JkFundFlow.class);
    }

    @Test
    public void resolvesReverseDeductFilterToLegacyAndCurrentCodes() {
        List<String> current = JkWithdrawAdminController.resolveFundFlowTypes("REVERSE_DEDUCT");
        List<String> legacy = JkWithdrawAdminController.resolveFundFlowTypes("COMMISSION_REVERSE_OUT");
        List<String> expected = Arrays.asList("REVERSE_DEDUCT", "COMMISSION_REVERSE_OUT");

        Assert.assertEquals(expected, current);
        Assert.assertEquals(expected, legacy);
    }

    @Test
    public void keepsNormalFundFlowTypeAsSingleValue() {
        Assert.assertEquals(Arrays.asList("WITHDRAW_FREEZE"), JkWithdrawAdminController.resolveFundFlowTypes("WITHDRAW_FREEZE"));
    }

    @Test
    public void returnsAllAccountScopedRecordsInOneDetailResponse() {
        JkWithdrawAdminController controller = new JkWithdrawAdminController();
        JkCommissionAccount account = new JkCommissionAccount().setId(9L).setUserId(46L).setRoleCode("county_agent");
        ReflectionTestUtils.setField(controller, "commissionAccountDao", proxy(JkCommissionAccountDao.class, account));
        ReflectionTestUtils.setField(controller, "recordDao", proxy(JkCommissionRecordDao.class,
                Collections.singletonList(new JkCommissionRecord().setId(11L).setReceiverUserId(46L).setReceiverRoleCode("county_agent"))));
        ReflectionTestUtils.setField(controller, "commissionFlowDao", proxy(JkCommissionFlowDao.class,
                Collections.singletonList(new JkCommissionFlow().setAccountId(9L))));
        ReflectionTestUtils.setField(controller, "reverseDao", proxy(JkCommissionReverseDao.class,
                Collections.singletonList(new JkCommissionReverse().setOriginalCommissionRecordId(11L))));
        JkFundAccount fundAccount = new JkFundAccount().setId(15L).setUserId(46L).setRoleCode("county_agent");
        ReflectionTestUtils.setField(controller, "fundAccountDao", proxy(JkFundAccountDao.class, fundAccount));
        ReflectionTestUtils.setField(controller, "fundFlowDao", proxy(JkFundFlowDao.class,
                Collections.singletonList(new JkFundFlow().setAccountId(15L))));
        JkUserContext context = new JkUserContext();
        context.getPermissions().add("platform.all");
        ReflectionTestUtils.setField(controller, "userContextService", contextService(context));

        CommonResult<Map<String, Object>> response = controller.commissionAccountDetail(9L);

        Assert.assertEquals(account, response.getData().get("account"));
        Assert.assertEquals(1, ((java.util.List<?>) response.getData().get("commissionRecords")).size());
        Assert.assertEquals(1, ((java.util.List<?>) response.getData().get("commissionFlows")).size());
        Assert.assertEquals(1, ((java.util.List<?>) response.getData().get("reverses")).size());
        Assert.assertEquals(fundAccount, response.getData().get("fundAccount"));
        Assert.assertEquals(1, ((java.util.List<?>) response.getData().get("fundFlows")).size());
    }

    @Test
    public void returnsLinkedCommissionDataWhenStartingFromFundAccount() {
        JkWithdrawAdminController controller = new JkWithdrawAdminController();
        JkFundAccount fundAccount = new JkFundAccount().setId(15L).setUserId(46L).setRoleCode("county_agent");
        ReflectionTestUtils.setField(controller, "fundAccountDao", proxy(JkFundAccountDao.class, fundAccount));
        ReflectionTestUtils.setField(controller, "commissionAccountDao", proxy(JkCommissionAccountDao.class,
                new JkCommissionAccount().setId(9L).setUserId(46L).setRoleCode("county_agent")));
        ReflectionTestUtils.setField(controller, "recordDao", proxy(JkCommissionRecordDao.class,
                Collections.singletonList(new JkCommissionRecord().setId(11L).setReceiverUserId(46L).setReceiverRoleCode("county_agent"))));
        ReflectionTestUtils.setField(controller, "commissionFlowDao", proxy(JkCommissionFlowDao.class,
                Collections.singletonList(new JkCommissionFlow().setAccountId(9L))));
        ReflectionTestUtils.setField(controller, "reverseDao", proxy(JkCommissionReverseDao.class, Collections.emptyList()));
        ReflectionTestUtils.setField(controller, "fundFlowDao", proxy(JkFundFlowDao.class,
                Collections.singletonList(new JkFundFlow().setAccountId(15L))));
        JkUserContext context = new JkUserContext();
        context.getPermissions().add("platform.all");
        ReflectionTestUtils.setField(controller, "userContextService", contextService(context));

        CommonResult<Map<String, Object>> response = controller.fundAccountDetail(15L);

        Assert.assertEquals(fundAccount, response.getData().get("fundAccount"));
        Assert.assertEquals(1, ((java.util.List<?>) response.getData().get("commissionRecords")).size());
        Assert.assertEquals(1, ((java.util.List<?>) response.getData().get("fundFlows")).size());
    }

    private <T> T proxy(Class<T> type, Object value) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, (instance, method, args) -> {
            if ("selectById".equals(method.getName()) || "selectOne".equals(method.getName())) return value;
            if ("selectList".equals(method.getName())) return value;
            return null;
        }));
    }

    private JkUserContextService contextService(JkUserContext context) {
        return proxy(JkUserContextService.class, context, "getAdminContext");
    }

    private <T> T proxy(Class<T> type, Object value, String methodName) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, (instance, method, args) ->
                methodName.equals(method.getName()) ? value : null));
    }
}
