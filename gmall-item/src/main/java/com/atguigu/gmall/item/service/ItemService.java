package com.atguigu.gmall.item.service;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.item.feign.GmallPmsClient;
import com.atguigu.gmall.item.feign.GmallSmsClient;
import com.atguigu.gmall.item.feign.GmallWmsClient;
import com.atguigu.gmall.item.vo.ItemVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.GroupVO;
import com.atguigu.gmall.sms.vo.ItemSaleVO;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import jdk.internal.org.objectweb.asm.Handle;
import net.bytebuddy.asm.Advice;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class ItemService {

    @Autowired
    private GmallPmsClient gmallPmsClient;
    @Autowired
    private GmallSmsClient gmallSmsClient;
    @Autowired
    private GmallWmsClient gmallWmsClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    public ItemVo item(Long skuId) {
        ItemVo itemVo = new ItemVo();

        // 1.查询sku信息
        CompletableFuture<SkuInfoEntity> skuCompletableFuture = CompletableFuture.supplyAsync(() -> {
            Resp<SkuInfoEntity> skuInfoEntityResp = this.gmallPmsClient.querySkuById(skuId);
            SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
            BeanUtils.copyProperties(skuInfoEntity, itemVo);
            return skuInfoEntity;
        }, threadPoolExecutor);

        // 2.品牌
        CompletableFuture<Void> brandCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            Resp<BrandEntity> brandEntityResp = this.gmallPmsClient.queryBrandById(skuInfoEntity.getBrandId());
            itemVo.setBrand(brandEntityResp.getData());
        }, threadPoolExecutor);


        // 3.分类
        CompletableFuture<Void> categoryCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            Resp<CategoryEntity> categoryEntityResp = this.gmallPmsClient.queryCategoryById(skuInfoEntity.getCatalogId());
            itemVo.setCategory(categoryEntityResp.getData());
        }, threadPoolExecutor);

        // 4.spu信息
        CompletableFuture<Void> spuCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            Resp<SpuInfoEntity> spuInfoEntityResp = this.gmallPmsClient.querySpuById(skuInfoEntity.getSpuId());
            itemVo.setSpuInfo(spuInfoEntityResp.getData());
        }, threadPoolExecutor);

        // 5.设置图片信息
        CompletableFuture<Void> picCompletableFuture = CompletableFuture.runAsync(() -> {
            Resp<List<String>> picsResp = this.gmallPmsClient.queryPicsBySkuId(skuId);
            itemVo.setPics(picsResp.getData());
        }, threadPoolExecutor);

        // 6.营销信息
        CompletableFuture<Void> saleCompletableFuture = CompletableFuture.runAsync(() -> {
            Resp<List<ItemSaleVO>> itemSaleResp = this.gmallSmsClient.queryItemSaleVOs(skuId);
            itemVo.setSales(itemSaleResp.getData());
        }, threadPoolExecutor);

        // 7.设置是否有货
        CompletableFuture<Void> storeCompletableFuture = CompletableFuture.runAsync(() -> {
            Resp<List<WareSkuEntity>> wareResp = this.gmallWmsClient.queryWareBySkuId(skuId);
            List<WareSkuEntity> wareSkuEntities = wareResp.getData();
            itemVo.setStore(wareSkuEntities.stream().anyMatch(t -> t.getStock() > 0));
        }, threadPoolExecutor);

        // 8.设置spu所有的销售属性
        CompletableFuture<Void> spuSaleCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            Resp<List<SkuSaleAttrValueEntity>> saleAttrValueResp = this.gmallPmsClient.querySaleAttrValues(skuInfoEntity.getSpuId());
            itemVo.setSkuSales(saleAttrValueResp.getData());
        }, threadPoolExecutor);

        // 9.spu的描述信息
        CompletableFuture<Void> descCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            Resp<SpuInfoDescEntity> spuInfoDescEntityResp = this.gmallPmsClient.queryDescById(skuInfoEntity.getSpuId());
            itemVo.setDesc(spuInfoDescEntityResp.getData());
        }, threadPoolExecutor);

        // 10.规格属性分组及组下的规格参数
        CompletableFuture<Void> groupCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            Resp<List<GroupVO>> listResp = this.gmallPmsClient.queryGroupVOById(skuInfoEntity.getCatalogId(), skuInfoEntity.getSpuId());
            itemVo.setGroups(listResp.getData());
        }, threadPoolExecutor);


        CompletableFuture.allOf(
                brandCompletableFuture,
                categoryCompletableFuture,
                spuCompletableFuture,
                picCompletableFuture,
                saleCompletableFuture,
                storeCompletableFuture,
                spuSaleCompletableFuture,
                descCompletableFuture,
                groupCompletableFuture).join();

        return  itemVo;
    }


    /*public static void main(String[] args) {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("初始化对象");
//            int i= 1/0;
            return "初始化对象";
        }).thenApply(t -> {
            System.out.println("thenApply");
            System.out.println("t；" + t);
            return "thenApply";
        }).whenComplete((t, u) -> {
            System.out.println("whenComplete");
            System.out.println("t；" + t);
            System.out.println("u：" + u);
        }).exceptionally(t -> {
            System.out.println("t；" + t);
            return "66666";
        }).handle((t, u) -> {
                    System.out.println("handle");
                    System.out.println("t：" + t);
                    System.out.println("u：" + u);
                    return "000";

        }).applyToEither(CompletableFuture.completedFuture("两个任务，只要有一个完成就完成"),(t) -> {
            System.out.println("completedFuture");
            System.out.println("t:"       + t);
            return "applyToEither";
        }).handle((t,u) -> {
            System.out.println("第二个任务只要有一个就完成的handle");
            System.out.println("t："            + t);
            System.out.println("u："            + u);
            return "完成";
        });
//        }).thenCombine(CompletableFuture.completedFuture("第二个任务,两个都要完成"),(t,u)->{
//            System.out.println("completedFuture");
//            System.out.println("t:"       + t);
//            System.out.println("u:"       + u);
//            return "thenCombine";
//        }).handle((t, u) -> {
//            System.out.println("第二个任务都完成的handle");
//            System.out.println("t："            + t);
//            System.out.println("u："            + u);
//            return "完成";
//        });
    }*/


}
