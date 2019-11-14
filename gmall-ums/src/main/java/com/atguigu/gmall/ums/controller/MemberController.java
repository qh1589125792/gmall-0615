package com.atguigu.gmall.ums.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.ums.consts.AppConsts;
import com.atguigu.gmall.ums.utils.ManagerUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.service.MemberService;




/**
 * 会员
 *
 * @author qinhan
 * @email 1589125792@qq.com
 * @date 2019-10-28 20:53:31
 */
@Api(tags = "会员 管理")
@RestController
@RequestMapping("ums/member")
public class MemberController {

    @Autowired
    private MemberService memberService;

//    @Autowired
//    private GmallMmsClient gmallMmsClient;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private AmqpTemplate amqpTemplate;


    @PostMapping("send")
    public Resp<String> sendMms(@RequestParam("phone")String phone){
        //最终返回响应的信息
        String msg;

        //验证手机号拿格式
        boolean b = ManagerUtils.isMobilePhone(phone);
        if(!b) {
            return Resp.fail("手机号码格式错误");
        }
        //验证redis中存储的当前手机号码获取验证次数
        //一个手机号码一天只能获取3次
        String countStr = stringRedisTemplate.opsForValue().get(AppConsts.CODE_PREFIX+phone+AppConsts.CODE_COUNT_SUFFIX);
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
        Boolean hasKey = stringRedisTemplate.hasKey(AppConsts.CODE_PREFIX+phone+AppConsts.CODE_CODE_SUFFIX);
        if(hasKey) {
            return Resp.fail("请不要频繁获取验证码");
        }
        //发送验证码
        //随机生成验证码
        //String code = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        //封装发送验证码请求参数的集合
        String type = "mobile";
        Map<String, Object> map = new HashMap<>();
        map.put("phone", phone);
        map.put("type", type);
        map.put("count",count);
        this.amqpTemplate.convertAndSend("GMALL-MMS-EXCHANGE","member."+type,map);

        msg = "验证码发送成功";
        return Resp.ok(msg);
    }





    @GetMapping("query")
    public Resp<MemberEntity> queryUser(@RequestParam("username") String username, @RequestParam("password") String password){
        MemberEntity memberEntity = this.memberService.queryUser(username,password);
        return Resp.ok(memberEntity);

    }

    @PostMapping("register")
    public Resp<Object> register(MemberEntity memberEntity,@RequestParam("code")String code){
        this.memberService.register(memberEntity,code);
        return Resp.ok(null);
    }


    @GetMapping("check/{data}/{type}")
    public Resp<Boolean> checkData(@PathVariable("data")String data,@PathVariable("type")Integer type){
        boolean b = this.memberService.checkData(data,type);
        return Resp.ok(b);

    }

    /**
     * 列表
     */
    @ApiOperation("分页查询(排序)")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('ums:member:list')")
    public Resp<PageVo> list(QueryCondition queryCondition) {
        PageVo page = memberService.queryPage(queryCondition);

        return Resp.ok(page);
    }


    /**
     * 信息
     */
    @ApiOperation("详情查询")
    @GetMapping("/info/{id}")
    @PreAuthorize("hasAuthority('ums:member:info')")
    public Resp<MemberEntity> info(@PathVariable("id") Long id){
		MemberEntity member = memberService.getById(id);

        return Resp.ok(member);
    }

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/save")
    @PreAuthorize("hasAuthority('ums:member:save')")
    public Resp<Object> save(@RequestBody MemberEntity member){
		memberService.save(member);

        return Resp.ok(null);
    }

    /**
     * 修改
     */
    @ApiOperation("修改")
    @PostMapping("/update")
    @PreAuthorize("hasAuthority('ums:member:update')")
    public Resp<Object> update(@RequestBody MemberEntity member){
		memberService.updateById(member);

        return Resp.ok(null);
    }

    /**
     * 删除
     */
    @ApiOperation("删除")
    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('ums:member:delete')")
    public Resp<Object> delete(@RequestBody Long[] ids){
		memberService.removeByIds(Arrays.asList(ids));

        return Resp.ok(null);
    }

}
