package com.atguigu.gmall.order.service;

import com.atguigu.core.bean.Resp;
import com.atguigu.core.bean.UserInfo;
import com.atguigu.gmall.cart.vo.CartItemVO;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVO;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.interceptor.LoginInterceptor;
import com.atguigu.gmall.order.vo.OrderConfirmVO;

import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVO;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.vo.SkuLockVO;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private GmallOmsClient gmallOmsClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AmqpTemplate amqpTemplate;

    private static final String TOKEN_PREFIX = "order:token:";

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
            this.redisTemplate.opsForValue().set(TOKEN_PREFIX+timeId,timeId);
        }, threadPoolExecutor);

        CompletableFuture.allOf(addressFuture,cartFuture,boundFuture,idFuture).join();
        return orderConfirmVO;
    }

    public void submit(OrderSubmitVO orderSubmitVO) {

        //1. 验证令牌防止重复提交
        String orderToken = orderSubmitVO.getOrderToken();
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long flag = this.redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(TOKEN_PREFIX + orderToken), orderToken);
        if(flag == 0L){
            throw new RuntimeException("请不要重复提交！！");

        }
        //2. 验证价格
        BigDecimal totalPrice = orderSubmitVO.getTotalPrice();
        List<OrderItemVo> orderItemVOS = orderSubmitVO.getOrderItemVOS();
        if(CollectionUtils.isEmpty(orderItemVOS)){
            throw new RuntimeException("请添加购物清单!!");
        }
        BigDecimal currentPrice = orderItemVOS.stream().map(orderItemVo -> {
            Resp<SkuInfoEntity> skuInfoEntityResp = this.gmallPmsClient.querySkuById(orderItemVo.getSkuId());
            SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
            return skuInfoEntity.getPrice().multiply(new BigDecimal(orderItemVo.getCount()));
        }).reduce((a, b) -> a.add(b)).get();
        if(totalPrice.compareTo(currentPrice) != 0){
            throw new RuntimeException("请刷新页面重试！！");
        }
        //3. 验证库存，并锁定库存
        List<SkuLockVO> skuLockVOS = orderItemVOS.stream().map(orderItemVo -> {
            SkuLockVO skuLockVO = new SkuLockVO();
            skuLockVO.setSkuId(orderItemVo.getSkuId());
            skuLockVO.setCount(orderItemVo.getCount());
            skuLockVO.setOrderToken(orderToken);
            return skuLockVO;
        }).collect(Collectors.toList());
        Resp<Object> objectResp = this.gmallWmsClient.checkAndLock(skuLockVOS);
        if(objectResp.getCode() == 1){
            throw new RuntimeException(objectResp.getMsg());
        }
        //4. 生成订单
        UserInfo userInfo = LoginInterceptor.get();
        try {
            orderSubmitVO.setUserId(userInfo.getUserId());
            Resp<MemberEntity> memberEntityResp = this.gmallUmsClient.queryUserById(userInfo.getUserId());
            MemberEntity memberEntity = memberEntityResp.getData();
            orderSubmitVO.setUserName(memberEntity.getUsername());
            Resp<OrderEntity> orderResp = this.gmallOmsClient.createOrder(orderSubmitVO);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("订单创建失败!服务器异常！");
        }
        //5. 删购物车中对应的记录（消息队列）
        Map<String, Object> map = new HashMap<>();
        map.put("userId",userInfo.getUserId());
        List<Long> skuIds = orderItemVOS.stream().map(orderItemVo -> orderItemVo.getSkuId()).collect(Collectors.toList());
        map.put("skuIds",skuIds);
        this.amqpTemplate.convertAndSend("GMALL-ORDER-EXCHANGE","cart.delete",map);
    }
}
