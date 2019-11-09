package com.atguigu.gmall.index.service;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.CategoryVO;

import java.util.List;

public interface IndexService {
    List<CategoryEntity> queryLevel1Categroy();

    List<CategoryVO> queryCategoryVO(Long pid);

    String testLock();

    String read();

    String write();

    String latch() throws InterruptedException;

    String out();
}
