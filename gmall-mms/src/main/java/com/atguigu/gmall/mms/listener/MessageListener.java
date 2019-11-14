package com.atguigu.gmall.mms.listener;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.mms.consts.AppConsts;
import com.atguigu.gmall.mms.templates.SmsTemplate;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class MessageListener {

    @Autowired
    private SmsTemplate smsTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "GMALL-MMS-QUEUE",durable = "true"),
            exchange = @Exchange(
                    name = "GMALL-MMS-EXCHANGE",
                    ignoreDeclarationExceptions = "true",
                    type= ExchangeTypes.TOPIC),
            key = {"member.*"}
    ))
    public void listener(Map<String, Object> map){

        String msg;

        //解析querys获取手机号，type
        String phone = (String) map.get("phone");
        String type = (String) map.get("type");
        Integer count = (Integer) map.get("count");

        Map<String, String> querys = new HashMap<>();
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        querys.put("mobile", phone);
        querys.put("param", AppConsts.CODE_PREFIX+code);
        querys.put("tpl_id", "TP1711063");
        /*
            此处为发送短信验证码的地方
         */
        Boolean sendSms = smsTemplate.sendSms(querys);
        if(!sendSms) {
            msg = "验证码发送失败，请重试";
            Resp.fail(msg);
        }
        //将验证码存到redis中5分钟
        String codeKey = AppConsts.CODE_PREFIX+phone+AppConsts.CODE_CODE_SUFFIX;
        String countKey = AppConsts.CODE_PREFIX+phone+AppConsts.CODE_COUNT_SUFFIX;
        stringRedisTemplate.opsForValue().set(codeKey, code, 5, TimeUnit.MINUTES);
        //获取次数过期时间

        Long expire = stringRedisTemplate.getExpire(countKey, TimeUnit.MINUTES);
        if(expire == null || expire <= 0) {
            expire = (long) (24*60);
        }
        count++;
        //修改该手机号码发送的次数
        stringRedisTemplate.opsForValue().set(AppConsts.CODE_PREFIX+phone+AppConsts.CODE_COUNT_SUFFIX, count+"", expire, TimeUnit.MINUTES);


    }
}
