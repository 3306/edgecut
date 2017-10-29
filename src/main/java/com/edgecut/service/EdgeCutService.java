package com.edgecut.service;

import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.edgecut.entity.CutDataDO;
import com.edgecut.mapper.CutDataMapper;
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
    @Resource
    private CutDataMapper cutDataMapper;

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
                runAsync(prefix, objectSummary.getKey());
                cnt ++;
            }
        } while (ls.isTruncated());
        return cnt;
    }

    public void runSync(String prefix, String filePath){
        String diskFilePath = ossDir + "/" + filePath;
        try {
            List<Integer> result = work(diskFilePath);
            if (result == null || result.size() != 4){
                logger.error("work failed. filePath = {}", filePath);
                return;
            }
            CutDataDO cutDataDO = new CutDataDO();
            cutDataDO.setKey(filePath);
            cutDataDO.setPrefix(prefix);
            cutDataDO.setX(result.get(0));
            cutDataDO.setY(result.get(1));
            cutDataDO.setW(result.get(2));
            cutDataDO.setH(result.get(3));
            cutDataDO.setStatus(1);
            cutDataDO.setDeleteMark(0);
            cutDataMapper.save(cutDataDO);
//            String edge = String.format("%d/%d/%d/%d", result.get(0), result.get(1), result.get(2), result.get(3));
//            ossUtil.addMetaData(filePath, "edge", edge);
//            ossUtil.addMetaData(filePath, "cutstatus", "0");
        } catch (Throwable e) {
            logger.error("work error. filePath = {}", filePath, e);
        }
    }

    public void runAsync(String prefix, String filePath){
        threadPoolExecutor.execute(() -> runSync(prefix, filePath));
    }

    private List<Integer> work(String filePath) throws IOException {
        String commandStr = executor;
        logger.info("command : {}", commandStr);
        Process p = Runtime.getRuntime().exec(commandStr);
        if (!commandStr.startsWith("echo")) {
            try (OutputStream outputStream = p.getOutputStream()) {
                outputStream.write((filePath + "\n").getBytes());
                outputStream.flush();
            }
        }

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

        scanner.close();

        if (cnt != 5){
            logger.error("executor failed. cnt = {} filePath = {}", cnt, filePath);
            try (Scanner errScanner = new Scanner(p.getErrorStream())) {
                while (errScanner.hasNext()){
                    logger.warn("executor errMsg : {}", errScanner.nextLine());
                }
                logger.warn("executor errMsg finish");
            }
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
//        ObjectListing objectListing;
//        String nextMarker = null;
        String fileName = String.format("%s-%d.zip", prefix.replace("/", ""), System.currentTimeMillis());
        DownloadTask downloadTask = new DownloadTask(ossDir + "/" + fileName);
        downloadTask.setTargetUrl(fileName);

        CutDataDO queryDO = new CutDataDO();
        queryDO.setPrefix(prefix);
        List<CutDataDO> cutDataDOS = cutDataMapper.query(queryDO);
        for (CutDataDO cutDataDO : cutDataDOS) {
            downloadTask.getAllCnt().incrementAndGet();
            downloadAsync(downloadTask, cutDataDO);
        }
        downloadTask.setFinish(true);
        downloadAsync(downloadTask, null);

//        do {
//            objectListing = ossUtil.ls(prefix, nextMarker, 10);
//            for (OSSObjectSummary summary : objectListing.getObjectSummaries()) {
//                if (prefix.equals(summary.getKey())){
//                    continue;
//                }
//                downloadTask.getAllCnt().incrementAndGet();
//                downloadAsync(downloadTask, summary.getKey());
//            }
//            nextMarker = objectListing.getNextMarker();
//        } while (objectListing.isTruncated());
//        downloadTask.setFinish(true);
//        downloadAsync(downloadTask, null);
        return downloadTask;
    }

    public void downloadSync(DownloadTask downloadTask, CutDataDO cutDataDO){
        try {
            if (cutDataDO == null){
                return;
            }
            if (cutDataDO.getStatus() == 0) {
                return;
            }
            int x = cutDataDO.getX();
            int y = cutDataDO.getY();
            int w = cutDataDO.getW();
            int h = cutDataDO.getH();
            String style = String.format("image/crop,x_%d,y_%d,w_%d,h_%d", x, y, w, h);
            download(downloadTask, cutDataDO.getKey(), style);
        } catch (Exception e){
            logger.error("download error. cutDataDO = {}", cutDataDO, e);
        } finally {
            downloadTask.tryClose(cutDataDO);
            logger.info("download report: cutDataDO = {} allFile = {} finishFile = {} finish = {}",
                    cutDataDO,
                    downloadTask.getAllCnt(),
                    downloadTask.getFinishCnt(),
                    downloadTask.getFinish());
        }

    }

    public void downloadAsync(DownloadTask downloadTask, CutDataDO cutDataDO){
        threadPoolExecutor.execute(() -> downloadSync(downloadTask, cutDataDO));
    }

    private void download(DownloadTask downloadTask, String key, String style) throws IOException {
        File toZip = File.createTempFile("toZip", ".tif");
        ossUtil.getObject(key, style, toZip);
        downloadTask.nextEntry(key.substring(key.indexOf("/") + 1), toZip);
    }

}
