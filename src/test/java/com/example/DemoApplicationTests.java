package com.example;

import com.Application;
import com.EdgeCutService;
import com.OssUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
public class DemoApplicationTests {

	@Resource
	private EdgeCutService edgeCutService;
	@Resource
	private OssUtil ossUtil;

	@Test
	public void contextLoads() {
	}

	@Test
	public void testEdgeCut() throws InterruptedException {
		edgeCutService.runAsync("test/047.tif");
		Thread.sleep(10000000L);
	}

	@Test
	public void testBatchRun() throws InterruptedException {
		edgeCutService.batchRun("test/");
		Thread.sleep(10000000L);
	}

	@Test
	public void testGetBaseDir(){
		System.out.println(ossUtil.getBaseDir());
	}

}
