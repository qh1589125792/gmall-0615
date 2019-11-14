package com.atguigu.gmall.ums.service.impl;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.mms.consts.AppConsts;
import com.atguigu.gmall.ums.feign.GmallMmsClient;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.ums.dao.MemberDao;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

//    @Autowired
//    private GmallMmsClient gmallMmsClient;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public boolean checkData(String data, Integer type) {
        QueryWrapper<MemberEntity> wrapper = new QueryWrapper<>();
        switch (type){
            case 1:
                wrapper.eq("username" ,data);
                break;
            case 2:
                wrapper.eq("mobile" ,data);
                break;
            case 3:
                wrapper.eq("email" ,data);
                break;
            default: break;
        }
        return this.count(wrapper) == 0;

    }

    @Override
    public void register(MemberEntity memberEntity, String code) {

        //1、校验验证码
       // this.gmallMmsClient.sendSms(memberEntity.getMobile());

        String mobile = memberEntity.getMobile();

        String redisCode = this.stringRedisTemplate.opsForValue().get(AppConsts.CODE_PREFIX + mobile + AppConsts.CODE_CODE_SUFFIX);
        if (StringUtils.isEmpty(redisCode)){
            return;
        }
        if(!redisCode.equals(code)){
            return;
        }

        //2、生成盐
        String salt = StringUtils.substring(UUID.randomUUID().toString(), 0, 6);
        memberEntity.setSalt(salt);

        //3、加盐加密
        memberEntity.setPassword(DigestUtils.md5Hex(memberEntity.getPassword()+salt));

        //4、注册功能
        memberEntity.setLevelId(1L);
        memberEntity.setStatus(1);
        memberEntity.setCreateTime(new Date());
        memberEntity.setIntegration(0);
        memberEntity.setGrowth(0);
        this.save(memberEntity);

        //5、删除Redis中的验证码
//        this.stringRedisTemplate.delete(AppConsts.CODE_PREFIX + code + AppConsts.CODE_CODE_SUFFIX);

    }

    @Override
    public MemberEntity queryUser(String username, String password) {

        MemberEntity memberEntity = this.getOne(new QueryWrapper<MemberEntity>().eq("username", username));
        //如果根据用户名查询的用户不存在说明用户名不合法，抛出异常
        if (memberEntity == null){
            throw new IllegalArgumentException("用户名密码错误");
        }

        //对用户输入的密码进行加密
        password = DigestUtils.md5Hex(password + memberEntity.getSalt());
        if (!StringUtils.equals(password,memberEntity.getPassword())){
            throw new IllegalArgumentException("用户名密码错误");
        }
        return memberEntity;
    }

}