package com.example;

import com.edgecut.mapper.CutDataMapper;
import org.junit.Test;

import javax.annotation.Resource;

public class MybatisTest extends DemoApplicationTests {

    @Resource
    private CutDataMapper cutDataMapper;

    @Test
    public void test(){
        cutDataMapper.query(null);
    }
}
