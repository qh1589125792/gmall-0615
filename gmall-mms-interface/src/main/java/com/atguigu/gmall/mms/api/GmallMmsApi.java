package com.atguigu.gmall.mms.api;

import com.atguigu.core.bean.Resp;
import org.springframework.web.bind.annotation.PostMapping;


public interface GmallMmsApi {

    //1.给手机号码发送短信验证码
    @PostMapping("user/sendSms")
    public Resp<Object> sendSms(String phoneNum);
}
