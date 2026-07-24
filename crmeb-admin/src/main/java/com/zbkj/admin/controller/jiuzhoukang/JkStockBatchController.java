package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.*;
import com.zbkj.common.model.jiuzhoukang.JkStockBatch;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockBatchUpdateRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
import com.zbkj.service.service.jiuzhoukang.stock.StockBatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** 第六阶段库存批次、库龄和 FIFO 初始化入口。 */
@RestController
@RequestMapping("api/admin/jk/stock-batch")
public class JkStockBatchController {
    @Autowired private StockBatchService service;
    @Autowired private JkAdminActorService actor;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('"+ JkPermissionCodes.ADMIN_STOCK_BATCH_LIST +"')")
    @JkBizPermission(value= JkBizPermissionCodes.STOCK_BATCH_VIEW)
    public CommonResult<CommonPage<JkStockBatch>> list(@RequestParam(required=false)Long stockAccountId,
                                                        @RequestParam(required=false)Integer productId,
                                                        @RequestParam(required=false)Integer skuId,
                                                        @RequestParam(required=false)String agingLevel,
                                                        PageParamRequest page){
        return CommonResult.success(CommonPage.restPage(service.list(stockAccountId,productId,skuId,agingLevel,page)));
    }

    @PostMapping("/metadata")
    @PreAuthorize("hasAuthority('"+ JkPermissionCodes.ADMIN_STOCK_BATCH_UPDATE +"')")
    @JkBizPermission(value= JkBizPermissionCodes.STOCK_BATCH_MANAGE)
    public CommonResult<JkStockBatch> updateMetadata(@RequestBody JkStockBatchUpdateRequest request){
        if(!actor.isPlatformSuperAdmin(actor.getCurrentAdmin()))throw new IllegalStateException("仅平台超级管理员可以维护批次成本和有效期");
        return CommonResult.success(service.updateMetadata(-Long.valueOf(actor.getCurrentAdmin().getId()),request));
    }

    /**
     * 只允许平台超管执行一次历史库存批次初始化。
     * 执行前 SQL 审计必须保证 frozen_qty=0，否则无法还原历史冻结批次。
     */
    @PostMapping("/opening-init")
    @PreAuthorize("hasAuthority('"+ JkPermissionCodes.ADMIN_STOCK_BATCH_INIT +"')")
    @JkBizPermission(value= JkBizPermissionCodes.STOCK_BATCH_MANAGE)
    public CommonResult<Integer> openingInit(){
        if(!actor.isPlatformSuperAdmin(actor.getCurrentAdmin()))throw new IllegalStateException("仅平台超级管理员可以初始化历史库存批次");
        return CommonResult.success(service.openingFromStockItems(-Long.valueOf(actor.getCurrentAdmin().getId())));
    }
}
