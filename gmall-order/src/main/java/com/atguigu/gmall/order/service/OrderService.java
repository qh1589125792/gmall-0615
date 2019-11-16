package com.atguigu.gmall.order.service;

import com.atguigu.core.bean.Resp;
import com.atguigu.core.bean.UserInfo;
import com.atguigu.gmall.cart.vo.CartItemVO;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.interceptor.LoginInterceptor;
import com.atguigu.gmall.order.vo.OrderConfirmVO;
import com.atguigu.gmall.order.vo.OrderItemVo;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVO;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private GmallPmsClient gmallPmsClient;

    @Autowired
    private GmallSmsClient gmallSmsClient;

    @Autowired
    private GmallWmsClient gmallWmsClient;

    @Autowired
    private GmallUmsClient gmallUmsClient;

    @Autowired
    private GmallCartClient gmallCartClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    public OrderConfirmVO confirm() {
        OrderConfirmVO orderConfirmVO = new OrderConfirmVO();

        //获取用户登录的信息
        UserInfo userInfo = LoginInterceptor.get();

        //查询用户的收货地址列表
        CompletableFuture<Void> addressFuture = CompletableFuture.runAsync(() -> {
            Resp<List<MemberReceiveAddressEntity>> addressResp = this.gmallUmsClient.queryAddressByUserId(userInfo.getUserId());
            orderConfirmVO.setAddresses(addressResp.getData());
        }, threadPoolExecutor);

        //获取购物车中选择中的记录
        CompletableFuture<Void> cartFuture = CompletableFuture.supplyAsync(() -> {
            Resp<List<CartItemVO>> listResp = this.gmallCartClient.queryCartItemVO(userInfo.getUserId());
            List<CartItemVO> itemVOS = listResp.getData();
            return itemVOS;
        }, threadPoolExecutor).thenAcceptAsync(itemVOS -> {
            if (CollectionUtils.isEmpty(itemVOS)) {
                return;
            }
            // 把购物车选中的记录转化成订货清单
            List<OrderItemVo> orderItems = itemVOS.stream().map(cartItemVO -> {
                //创建订单详情
                OrderItemVo orderItemVo = new OrderItemVo();

                //根据skuId查询sku
                Resp<SkuInfoEntity> skuInfoEntityResp = this.gmallPmsClient.querySkuById(cartItemVO.getSkuId());
                SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
                orderItemVo.setTitle(skuInfoEntity.getSkuTitle());
                orderItemVo.setPrice(skuInfoEntity.getPrice());
                orderItemVo.setDefaultImage(skuInfoEntity.getSkuDefaultImg());
                orderItemVo.setWeight(skuInfoEntity.getWeight());

                //根据skuId查询销售属性
                Resp<List<SkuSaleAttrValueEntity>> skusaleResp = this.gmallPmsClient.querySaleAttrBySkuId(cartItemVO.getSkuId());
                List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = skusaleResp.getData();
                orderItemVo.setSkuAttrValue(skuSaleAttrValueEntities);

                orderItemVo.setSkuId(cartItemVO.getSkuId());
                orderItemVo.setCount(cartItemVO.getCount());

                //根据skuId获取营销信息
                Resp<List<ItemSaleVO>> saleResp = this.gmallSmsClient.queryItemSaleVOs(cartItemVO.getSkuId());
                List<ItemSaleVO> itemSaleVOS = saleResp.getData();
                orderItemVo.setSales(itemSaleVOS);

                //根据skuId获取库存信息
                Resp<List<WareSkuEntity>> storeResp = this.gmallWmsClient.queryWareBySkuId(cartItemVO.getSkuId());
                List<WareSkuEntity> wareSkuEntities = storeResp.getData();
                orderItemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0));
                return orderItemVo;
            }).collect(Collectors.toList());
            orderConfirmVO.setOrderItems(orderItems);
        }, threadPoolExecutor);

        //获取用户信息（积分）
        CompletableFuture<Void> boundFuture = CompletableFuture.runAsync(() -> {
            Resp<MemberEntity> memberEntityResp = this.gmallUmsClient.queryUserById(userInfo.getUserId());
            MemberEntity memberEntity = memberEntityResp.getData();
            orderConfirmVO.setBounds(memberEntity.getIntegration());
        }, threadPoolExecutor);

        //生成唯一标准，防重复提交
        CompletableFuture<Void> idFuture = CompletableFuture.runAsync(() -> {
            String timeId = IdWorker.getTimeId();
            orderConfirmVO.setOrderToken(timeId);
        }, threadPoolExecutor);

        CompletableFuture.allOf(addressFuture,cartFuture,boundFuture,idFuture).join();
        return orderConfirmVO;
    }
}
