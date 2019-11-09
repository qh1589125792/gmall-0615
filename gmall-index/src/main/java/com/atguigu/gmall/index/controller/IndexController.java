package com.atguigu.gmall.index.controller;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.CategoryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("index")
public class IndexController {
    @Autowired
    private IndexService indexService;



    @GetMapping("cates")
    public Resp<List<CategoryEntity>> queryLevel1Categroy(){

        List<CategoryEntity> categoryEntities = this.indexService.queryLevel1Categroy();
        return Resp.ok(categoryEntities);

    }

    @GetMapping("cates/{pid}")
    public Resp<List<CategoryVO>> queryCategoryVO(@PathVariable("pid")Long pid){
        List<CategoryVO> categoryVOS =  this.indexService.queryCategoryVO(pid);
        return Resp.ok(categoryVOS);
    }


    @GetMapping("testLock")
    public Resp<Object> testLock(HttpServletRequest request){
        System.out.println(request.getLocalPort());
        String msg = this.indexService.testLock();
        return  Resp.ok(msg);
    }

    @GetMapping("read")
    public Resp<String> read(){
        String msg = this.indexService.read();
        return Resp.ok(msg);
    }

    @GetMapping("write")
    public Resp<String> write(){
        String msg = this.indexService.write();
        return Resp.ok(msg);
    }

    @GetMapping("latch")
    public Resp<Object> latch() throws InterruptedException{
        String msg = this.indexService.latch();
        return Resp.ok(msg);
    }

    @GetMapping("out")
    public Resp<Object> out(){
        String msg = this.indexService.out();
        return Resp.ok(msg);
    }
}
