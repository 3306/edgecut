package com;

import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringJoiner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class EdgeCutService {

    @Resource
    private String executor;
    @Resource
    private OssUtil ossUtil;

    private String ossDir = System.getProperty("ossDir");

    private ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(10, 10, 1, TimeUnit.MINUTES, new LinkedBlockingQueue<>(), new ThreadPoolExecutor.CallerRunsPolicy());

    private Logger logger = LoggerFactory.getLogger(getClass());

    public void runSync(String filePath){
        String diskFilePath = ossDir + "/" + filePath;
        try {
            List<Integer> result = work(diskFilePath);
            if (result == null || result.size() != 4){
                logger.error("work failed. filePath = {}", filePath);
                return;
            }
            String edge = String.format("%d/%d/%d/%d", result.get(0), result.get(1), result.get(2), result.get(3));
            ossUtil.addMetaData(filePath, "edge", edge);
            ossUtil.addMetaData(filePath, "cutstatus", "0");
        } catch (Throwable e) {
            logger.error("work error. filePath = {}", filePath, e);
        }
    }

    public void runAsync(String filePath){
        threadPoolExecutor.execute(() -> runSync(filePath));
    }

    public int batchRun(String prefix){
        String next = null;
        ObjectListing ls;
        int cnt = 0;
        do {
            ls = ossUtil.ls(prefix, next, 1);
            next = ls.getNextMarker();

            List<OSSObjectSummary> objectSummaries = ls.getObjectSummaries();
            for (OSSObjectSummary objectSummary : objectSummaries) {
                if (objectSummary.getKey().equals(prefix)){
                    continue;
                }
                runAsync(objectSummary.getKey());
                cnt ++;
            }
        } while (ls.isTruncated());
        return cnt;
    }

    private List<Integer> work(String filePath) throws IOException {
        String commandStr = executor + " " + filePath;
        Process p = Runtime.getRuntime().exec(commandStr);
        Scanner scanner = new Scanner(p.getInputStream());

        String fileName = null;
        Integer x = -1, y = -1, w = -1, h = -1;
        int cnt = 0;
        if (scanner.hasNext()){
            fileName = scanner.next();
            cnt ++;
        }
        if (scanner.hasNext()){
            x = scanner.nextInt();
            cnt ++;
        }
        if (scanner.hasNext()){
            y = scanner.nextInt();
            cnt ++;
        }
        if (scanner.hasNext()){
            w = scanner.nextInt();
            cnt ++;
        }
        if (scanner.hasNext()){
            h = scanner.nextInt();
            cnt ++;
        }

        if (cnt != 5){
            logger.error("executor failed. filePath = {}", filePath);
            return null;
        }

        List<Integer> result = new ArrayList<>();
        result.add(x);
        result.add(y);
        result.add(w);
        result.add(h);
        logger.info("filePath = {} result = {}", fileName, result);
        return result;
    }
}
