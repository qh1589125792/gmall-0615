package com.atguigu.gmall.wms.vo;

import lombok.Data;

@Data
public class SkuLockVO {

    private Long skuId;

    private Integer count;

    private Boolean lock; //锁定成功

    private Long skuWareId; //锁定库存的id
}
