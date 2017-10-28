package com.edgecut.service;

import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.edgecut.oss.DownloadTask;
import com.edgecut.oss.OssUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.*;
import java.util.*;
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

    //切图

    public int batchRun(String prefix){
        String next = null;
        ObjectListing ls;
        int cnt = 0;
        do {
            ls = ossUtil.ls(prefix, next, 10);
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

    private List<Integer> work(String filePath) throws IOException {
        String commandStr = String.format("%s \"%s\"",executor, filePath);
        logger.info("command : {}", commandStr);
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

    //下载

    public DownloadTask batchDownload(String prefix){
        ObjectListing objectListing;
        String nextMarker = null;
        String fileName = String.format("%s-%d.zip", prefix.replace("/", ""), System.currentTimeMillis());
        DownloadTask downloadTask = new DownloadTask(ossDir + "/" + fileName);
        downloadTask.setTargetUrl(fileName);
        do {
            objectListing = ossUtil.ls(prefix, nextMarker, 10);
            for (OSSObjectSummary summary : objectListing.getObjectSummaries()) {
                if (prefix.equals(summary.getKey())){
                    continue;
                }
                downloadTask.getAllCnt().incrementAndGet();
                downloadAsync(downloadTask, summary.getKey());
            }
            nextMarker = objectListing.getNextMarker();
        } while (objectListing.isTruncated());
        downloadTask.setFinish(true);
        downloadAsync(downloadTask, null);
        return downloadTask;
    }

    public void downloadSync(DownloadTask downloadTask, String key){
        try {
            if (key == null){
                return;
            }
            String edge = ossUtil.getMetaData(key).getUserMetadata().get("edge");
            if (StringUtils.isBlank(edge)) {
                return;
            }
            String[] split = edge.split("/");
            if (split.length != 4) {
                return;
            }

            int x = Integer.valueOf(split[0]);
            int y = Integer.valueOf(split[1]);
            int w = Integer.valueOf(split[2]);
            int h = Integer.valueOf(split[3]);
            String style = String.format("image/crop,x_%d,y_%d,w_%d,h_%d", x, y, w, h);
            download(downloadTask, key, style);
        } catch (Exception e){
            logger.error("download error. key = {}", key, e);
        } finally {
            downloadTask.tryClose(key);
            logger.info("download report: key = {} allFile = {} finishFile = {} finish = {}",
                    key,
                    downloadTask.getAllCnt(),
                    downloadTask.getFinishCnt(),
                    downloadTask.getFinish());
        }

    }

    public void downloadAsync(DownloadTask downloadTask, String key){
        threadPoolExecutor.execute(() -> downloadSync(downloadTask, key));
    }

    private void download(DownloadTask downloadTask, String key, String style) throws IOException {
        File toZip = File.createTempFile("toZip", ".tif");
        ossUtil.getObject(key, style, toZip);
        downloadTask.nextEntry(key.substring(key.indexOf("/") + 1), toZip);
    }

}
