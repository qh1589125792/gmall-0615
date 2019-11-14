package com.atguigu.gmall.mms.controller;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.mms.consts.AppConsts;
import com.atguigu.gmall.mms.templates.SmsTemplate;
import com.atguigu.gmall.mms.utils.ManagerUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("user")
public class UserController {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    SmsTemplate smsTemplate;

    //1.给手机号码发送短信验证码
    @GetMapping("sendSms")
    public Resp<Object> sendSms(@RequestParam("phoneNum") String phoneNum) {
        //验证手机号拿格式
        boolean b = ManagerUtils.isMobilePhone(phoneNum);
        if(!b) {
            return Resp.fail("手机号码格式错误");
        }
        //验证redis中存储的当前手机号码获取验证次数
        //一个手机号码一天只能获取3次
        String countStr = stringRedisTemplate.opsForValue().get(AppConsts.CODE_PREFIX+phoneNum+AppConsts.CODE_COUNT_SUFFIX);
        int count = 0;
        if(!StringUtils.isEmpty(countStr)) {
            //如果数量字符串不为空，转为数字
            count = Integer.parseInt(countStr);
        }
        if(count>=3) {
            return Resp.fail("验证码次数超标");
        }
        //验证redis中当前手机号是否存在未过期的验证码
        //获取当前手机号码在redis中的验证码：如果为空，代表没有：  code::code
        //redis特点：键在值在，键亡值亡
        Boolean hasKey = stringRedisTemplate.hasKey(AppConsts.CODE_PREFIX+phoneNum+AppConsts.CODE_CODE_SUFFIX);
        if(hasKey) {
            return Resp.fail("请不要频繁获取验证码");
        }
        //发送验证码
        //随机生成验证码
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        //封装发送验证码请求参数的集合
        Map<String, String> querys = new HashMap<String, String>();
        querys.put("mobile", phoneNum);
        querys.put("param", AppConsts.CODE_PREFIX+code);
        querys.put("tpl_id", "TP1711063");


        /*
            此处为发送短信验证码的地方
         */
		Boolean sendSms = smsTemplate.sendSms(querys);
		if(!sendSms) {
			return Resp.fail("失败");
		}
        //将验证码存到redis中5分钟
        stringRedisTemplate.opsForValue().set(AppConsts.CODE_PREFIX+phoneNum+AppConsts.CODE_CODE_SUFFIX, code, 5, TimeUnit.MINUTES);
        //获取次数过期时间

        Long expire = stringRedisTemplate.getExpire(AppConsts.CODE_PREFIX+phoneNum+AppConsts.CODE_COUNT_SUFFIX , TimeUnit.MINUTES);
        if(expire == null || expire <= 0) {
            expire = (long) (24*60);
        }
        count++;
        //修改该手机号码发送的次数
        stringRedisTemplate.opsForValue().set(AppConsts.CODE_PREFIX+phoneNum+AppConsts.CODE_COUNT_SUFFIX, count+"", expire, TimeUnit.MINUTES);

        //响应成功
        return Resp.ok("验证码发送成功");

    }



}
