package com.atguigu.gmall.ums.feign;

import com.atguigu.gmall.mms.api.GmallMmsApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient("mms-service")
public interface GmallMmsClient extends GmallMmsApi {
}
