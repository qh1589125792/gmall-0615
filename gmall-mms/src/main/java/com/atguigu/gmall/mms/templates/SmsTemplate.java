package com.atguigu.gmall.mms.templates;

import java.util.HashMap;
import java.util.Map;

import com.atguigu.gmall.mms.utils.HttpUtils;

import org.apache.http.HttpResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SmsTemplate {

    @Value("${sms.host}") //只能在组件中使用
    private String host;
    @Value("${sms.path}") //只能在组件中使用
    private String path;
    @Value("${sms.method}") //只能在组件中使用
    private String method;
    @Value("${sms.appcode}") //只能在组件中使用
    private String appcode;
    //发送短息工具方法
    public Boolean sendSms(Map<String, String> querys) {
        Map<String, String> headers = new HashMap<String, String>();
        // 最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appcode);
        //参数值没次调用都不一样，需要作为参数传入
//		Map<String, String> querys = new HashMap<String, String>();
//		querys.put("mobile", "13849808656");
//		querys.put("param", "code:hahaha");
//		querys.put("tpl_id", "TP1711063");
        Map<String, String> bodys = new HashMap<String, String>();

        try {
            HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
            //System.out.println(response.toString());
            // 获取response的body
            // System.out.println(EntityUtils.toString(response.getEntity()));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
