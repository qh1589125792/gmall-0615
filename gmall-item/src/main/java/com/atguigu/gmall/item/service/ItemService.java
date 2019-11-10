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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItemService {

    @Autowired
    private GmallPmsClient gmallPmsClient;
    @Autowired
    private GmallSmsClient gmallSmsClient;
    @Autowired
    private GmallWmsClient gmallWmsClient;

    public ItemVo item(Long skuId) {
        ItemVo itemVo = new ItemVo();

        // 1.查询sku信息
        Resp<SkuInfoEntity> skuInfoEntityResp = this.gmallPmsClient.querySkuById(skuId);
        SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
        BeanUtils.copyProperties(skuInfoEntity,itemVo);
        Long spuId = skuInfoEntity.getSpuId();

        // 2.品牌
        Resp<BrandEntity> brandEntityResp = this.gmallPmsClient.queryBrandById(skuInfoEntity.getBrandId());
        itemVo.setBrand(brandEntityResp.getData());


        // 3.分类
        Resp<CategoryEntity> categoryEntityResp = this.gmallPmsClient.queryCategoryById(skuInfoEntity.getCatalogId());
        itemVo.setCategory(categoryEntityResp.getData());

        // 4.spu信息
        Resp<SpuInfoEntity> spuInfoEntityResp = this.gmallPmsClient.querySpuById(spuId);
        itemVo.setSpuInfo(spuInfoEntityResp.getData());

        // 5.设置图片信息
        Resp<List<String>> picsResp = this.gmallPmsClient.queryPicsBySkuId(skuId);
        itemVo.setPics(picsResp.getData());

        // 6.营销信息
        Resp<List<ItemSaleVO>> itemSaleResp = this.gmallSmsClient.queryItemSaleVOs(skuId);
        itemVo.setSales(itemSaleResp.getData());

        // 7.设置是否有货
        Resp<List<WareSkuEntity>> wareResp = this.gmallWmsClient.queryWareBySkuId(skuId);
        List<WareSkuEntity> wareSkuEntities = wareResp.getData();
        itemVo.setStore(wareSkuEntities.stream().anyMatch(t -> t.getStock()>0));

        // 8.设置spu所有的销售属性
        Resp<List<SkuSaleAttrValueEntity>> saleAttrValueResp = this.gmallPmsClient.querySaleAttrValues(spuId);
        itemVo.setSkuSales(saleAttrValueResp.getData());

        // 9.spu的描述信息
        Resp<SpuInfoDescEntity> spuInfoDescEntityResp = this.gmallPmsClient.queryDescById(spuId);
        itemVo.setDesc(spuInfoDescEntityResp.getData());

        // 10.规格属性分组及组下的规格参数
        Resp<List<GroupVO>> listResp = this.gmallPmsClient.queryGroupVOById(skuInfoEntity.getCatalogId(), spuId);
        itemVo.setGroups(listResp.getData());

        return  itemVo;
    }
}
