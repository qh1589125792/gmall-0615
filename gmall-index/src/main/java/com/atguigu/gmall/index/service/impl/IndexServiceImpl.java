package com.atguigu.gmall.index.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.index.annotation.GmallCache;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.CategoryVO;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class IndexServiceImpl implements IndexService {

    private static  final  String KEY_PREFIX = "index:category";

    private static  final  String KEY_INDEX = "index:index:";

    @Autowired
    private GmallPmsClient gmallPmsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public List<CategoryEntity> queryLevel1Categroy() {

        //查询缓存，缓存中有的话直接返回
        String indexLevel1 = this.redisTemplate.opsForValue().get(KEY_INDEX);
        if (StringUtils.isNoneBlank(indexLevel1)){
            return  JSON.parseArray(indexLevel1,CategoryEntity.class);
        }

        //2、如果缓存中没有，查询数据库
        Resp<List<CategoryEntity>> resp = this.gmallPmsClient.queryCategories(1, null);
        List<CategoryEntity> respData = resp.getData();

        //3、查询完成之后，放入缓存,
        this.redisTemplate.opsForValue().set(KEY_INDEX,JSON.toJSONString(respData));
        return respData;
    }

    @GmallCache(prefix = KEY_PREFIX,timeout = 300000L,random = 50000L)
    @Override
    public List<CategoryVO> queryCategoryVO(Long pid) {

        //1、查询缓存，缓存中有的话直接返回
//        String cache = this.redisTemplate.opsForValue().get(KEY_PREFIX + pid);
//        if(StringUtils.isNoneBlank(cache)){
//            return JSON.parseArray(cache,CategoryVO.class);
//        }

        //2、如果缓存中没有，查询数据库
        Resp<List<CategoryVO>> listResp = this.gmallPmsClient.queryCategoryWithSub(pid);
        List<CategoryVO> categoryVOS = listResp.getData();

        //3、查询完成之后，放入缓存,
//       this.redisTemplate.opsForValue().set(KEY_PREFIX + pid,JSON.toJSONString(categoryVOS),5 + (int)(Math.random() * 5),TimeUnit.DAYS);

        return categoryVOS;
    }












/*   @Override
    public  String testLock() {
        String uuid = UUID.randomUUID().toString();
        Boolean lock = this.redisTemplate.opsForValue().setIfAbsent("lock", uuid,10 ,TimeUnit.SECONDS);
        if (lock) {
            String numString = this.redisTemplate.opsForValue().get("num");
            if (StringUtils.isBlank(numString)) {
                return null;
            }
            int num = Integer.parseInt(numString);
            this.redisTemplate.opsForValue().set("num", String.valueOf(++num));

            //释放锁
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            this.redisTemplate.execute(new DefaultRedisScript<>(script,Long.class), Arrays.asList("lock"),uuid);
//            if(StringUtils.equals(uuid,this.redisTemplate.opsForValue().get("lock"))){
//                this.redisTemplate.delete("lock");
//            }

        }else {
            try {
                TimeUnit.SECONDS.sleep(1);

                //重新获取锁
                testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        return "已经增加成功";

    }*/


    @Override
    public  String testLock() {
        RLock lock = this.redissonClient.getLock("lock");
        lock.lock();


        String numString = this.redisTemplate.opsForValue().get("num");
        if (StringUtils.isBlank(numString)) {
            return null;
        }
        int num = Integer.parseInt(numString);
        this.redisTemplate.opsForValue().set("num", String.valueOf(++num));

        //释放锁
        lock.unlock();
        return "已经增加成功";

    }

    @Override
    public String read() {

        RReadWriteLock readWriteLock = this.redissonClient.getReadWriteLock("readWriteLock");
        RLock readLock = readWriteLock.readLock(); //获取读锁
        readLock.lock(10L,TimeUnit.SECONDS);
        String msg = this.redisTemplate.opsForValue().get("msg");
//        readLock.unlock();
        return msg;
    }

    @Override
    public String write() {

        RReadWriteLock readWriteLock = this.redissonClient.getReadWriteLock("readWriteLock");
        RLock writeLock = readWriteLock.writeLock();
        writeLock.lock(10L,TimeUnit.SECONDS);
        String msg = UUID.randomUUID().toString();
        this.redisTemplate.opsForValue().set("msg",msg);
//        writeLock.unlock();
        return "数据写如成功。。。。" + msg;
    }

    @Override
    public String latch() throws InterruptedException {
        RCountDownLatch latchDown = this.redissonClient.getCountDownLatch("latchDown");
//        String countString = this.redisTemplate.opsForValue().get("count");
//        int count = Integer.parseInt(countString);
        latchDown.trySetCount(5);
        latchDown.await();
        return "锁门。。。。。";

    }

    @Override
    public String out(){
        RCountDownLatch latchDown = this.redissonClient.getCountDownLatch("latchDown");
//        String countString = this.redisTemplate.opsForValue().get("count");
//        int count = Integer.parseInt(countString);
//        this.redisTemplate.opsForValue().set("count",String.valueOf(--count));
        latchDown.countDown();
        return "出来了一个人!!!!";

    }


}
