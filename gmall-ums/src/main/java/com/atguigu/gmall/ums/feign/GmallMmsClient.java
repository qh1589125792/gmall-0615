package com.atguigu.gmall.ums.feign;

import com.atguigu.core.bean.Resp;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("mms-service")
public interface GmallMmsClient {

    //1.给手机号码发送短信验证码
    @PostMapping("user/sendSms")
    public Resp<Object> sendSms(@RequestParam("phoneNum") String phoneNum);
}
