package com.edgecut.controller;

import com.aliyun.oss.model.OSSObjectSummary;
import com.edgecut.entity.CutDataDO;
import com.edgecut.entity.CutDataQTO;
import com.edgecut.mapper.CutDataMapper;
import com.edgecut.oss.CutResult;
import com.edgecut.oss.CutTask;
import com.edgecut.oss.DownloadTask;
import com.edgecut.oss.OssUtil;
import com.edgecut.service.EdgeCutService;
import com.edgecut.service.StsService;
import com.aliyun.oss.model.ObjectListing;
import com.aliyuncs.sts.model.v20150401.AssumeRoleResponse;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/service")
public class ServiceController{
    @Resource
    private EdgeCutService edgeCutService;
    @Resource
    private OssUtil ossUtil;
    @Resource
    private StsService stsService;
    @Resource
    private CutDataMapper cutDataMapper;

    private String ossUrl = "http://edgecut.oss-cn-shanghai.aliyuncs.com/";

    private Cache<String, DownloadTask> downloadCache = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES).build();

    private Cache<String, CutTask> runCache = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES).build();

    @RequestMapping("/run")
    @ResponseBody
    public synchronized CutTask batchRun(@RequestParam("prefix") String prefix) throws ExecutionException {
        if (!prefix.endsWith("/")){
            prefix = prefix + "/";
        }
        if (getRunStatus(prefix) != 0){
            throw new IllegalArgumentException("该批次已在运行中");
        }
        CutTask cutTask = edgeCutService.batchRun(prefix);
        runCache.put(prefix, cutTask);
        return cutTask;
    }

    @RequestMapping("/baseDir")
    @ResponseBody
    public List<String> baseDir(){
        return ossUtil.getBaseDir();
    }

    @RequestMapping("/dirCount")
    @ResponseBody
    public Map<String, Object> dirCount(@RequestParam("prefix") String prefix) throws ExecutionException {
        if (!prefix.endsWith("/")){
            prefix = prefix + "/";
        }
        Map<String, Object> result = new HashMap<>();
        result.putAll(count(prefix));
        result.put("runStatus", getRunStatus(prefix));
        result.put("downloadStatus", getDownloadStatus(prefix, result));

        return result;
    }

    private int getDownloadStatus(String prefix, Map<String, Object> result) throws ExecutionException {
        DownloadTask downloadTask = downloadCache.get(prefix, DownloadTask::new);
        if (downloadTask.getTargetUrl() != null){
            if (downloadTask.getClosed()){
                if (result != null){
                    result.put("downloadUrl", downloadTask.getTargetUrl());
                }
                return 0;
            } else {
                return 1;
            }
        } else {
            return 0;
        }
    }

    private int getRunStatus(String prefix) throws ExecutionException {
        CutTask cutTask = runCache.get(prefix, CutTask::new);
        if (cutTask.getPrefix() == null || cutTask.getAllCnt().intValue() == cutTask.getFinishCnt().intValue()){
            return 0;
        } else {
            return 1;
        }
    }

    private Map<String, AtomicInteger> count(String prefix) {
        CutDataQTO cutDataQTO = new CutDataQTO();
        cutDataQTO.setPrefix(prefix);
        List<CutDataDO> cutDataCountDOS = cutDataMapper.query(cutDataQTO);

        Map<String, AtomicInteger> count = new HashMap<>();
        Set<String> keys = new HashSet<>();
        for (CutDataDO cutDataDO : cutDataCountDOS) {
            keys.add(cutDataDO.getKey());
            count.computeIfAbsent("step-"+cutDataDO.getStatus(), str -> new AtomicInteger()).incrementAndGet();
        }

        //TODO 翻页处理
        ObjectListing ls = ossUtil.ls(prefix, null, 1000);
        for (OSSObjectSummary ossObjectSummary : ls.getObjectSummaries()) {
            if (keys.contains(ossObjectSummary.getKey())){
                continue;
            }
            if (ossObjectSummary.getKey().equals(prefix)){
                continue;
            }
            count.computeIfAbsent("step-0" , str -> new AtomicInteger()).incrementAndGet();
        }
        return count;
    }

    @RequestMapping("/download")
    @ResponseBody
    public synchronized DownloadTask download(@RequestParam("prefix") String prefix) throws ExecutionException {
        if (!prefix.endsWith("/")){
            prefix = prefix + "/";
        }
        if (getDownloadStatus(prefix, null) != 0){
            throw new IllegalArgumentException("该批次已在下载中。");
        }
        DownloadTask downloadTask = edgeCutService.batchDownload(prefix);
        downloadTask.setTargetUrl(ossUrl + downloadTask.getTargetUrl());
        downloadCache.put(downloadTask.getTargetUrl(), downloadTask);
        return downloadTask.toOutput();
    }

    @RequestMapping("/downloadStatus")
    @ResponseBody
    public DownloadTask downloadStatus(@RequestParam("key") String key) throws ExecutionException {
        return downloadCache.get(key, DownloadTask::new).toOutput();
    }

    @RequestMapping("/stsUpload")
    @ResponseBody
    public AssumeRoleResponse.Credentials stsUpload(){
        AssumeRoleResponse assumeRoleResponse = stsService.getToken();

        if (assumeRoleResponse != null){
            return assumeRoleResponse.getCredentials();
        }

        return null;
    }

//    @RequestMapping("/reject")
//    @ResponseBody
//    @Override
//    public void reject(@RequestParam("key") String key){
//        ossUtil.addMetaData(key, "cutstatus", "-1");
//    }

    @RequestMapping("/update")
    @ResponseBody
    public void update(@RequestParam("key") String key,
                       @RequestParam("x") String x,
                       @RequestParam("y") String y,
                       @RequestParam("w") String w,
                       @RequestParam("h") String h){
        String prefix = OssUtil.getPrefix(key);
        CutDataDO cutDataDO = new CutDataDO();
        cutDataDO.setPrefix(prefix);
        cutDataDO.setKey(key);
        cutDataDO.setX(Integer.valueOf(x));
        cutDataDO.setY(Integer.valueOf(y));
        cutDataDO.setW(Integer.valueOf(w));
        cutDataDO.setH(Integer.valueOf(h));
        cutDataDO.setStatus(2);
        cutDataDO.setDeleteMark(0);
        cutDataMapper.save(cutDataDO);
    }

    @RequestMapping("/result")
    @ResponseBody
    public Map<String, Object> getResult(@RequestParam("prefix") String prefixInput,
                                         @RequestParam(required = false, value = "currentPage", defaultValue = "1") String currentPage,
                                         @RequestParam(required = false, value = "pageSize", defaultValue = "50") String pageSize,
                                         @RequestParam(required = false, value = "status") String status
                                         ){
        final String prefix = prefixInput.endsWith("/") ? prefixInput : prefixInput + "/";
        CutDataQTO cutDataQTO = new CutDataQTO();
        cutDataQTO.setPrefix(prefix);
        cutDataQTO.setCurrentPage(Integer.valueOf(currentPage));
        cutDataQTO.setPageSize(Integer.valueOf(pageSize));
        if (status != null){
            cutDataQTO.setStatus(Integer.valueOf(status));
        }
        List<CutDataDO> cutDataDOS = cutDataMapper.query(cutDataQTO);
        Integer count = cutDataMapper.count(cutDataQTO);
        List<CutResult> cutResults = cutDataDOS.parallelStream().map(cutDataDO -> {
            CutResult cutResult = new CutResult(cutDataDO.getKey());
            cutResult.setOriginDownloadUrl(ossUrl + cutDataDO.getKey());
            cutResult.setOriginShowUrl(ossUrl + cutDataDO.getKey() + "?x-oss-process=image/format,webp/resize,w_800");
            cutResult.setStatus(cutDataDO.getStatus());
            cutResult.setX(cutDataDO.getX());
            cutResult.setY(cutDataDO.getY());
            cutResult.setW(cutDataDO.getW());
            cutResult.setH(cutDataDO.getH());
            cutResult.setCutShowUrl(ossUrl + cutDataDO.getKey() + String.format("?x-oss-process=image/crop,x_%d,y_%d,w_%d,h_%d/format,webp/resize,w_800", cutDataDO.getX(), cutDataDO.getY(), cutDataDO.getW(), cutDataDO.getH()));
            cutResult.setCutDownloadUrl(ossUrl + cutDataDO.getKey() + String.format("?x-oss-process=image/crop,x_%d,y_%d,w_%d,h_%d", cutDataDO.getX(), cutDataDO.getY(), cutDataDO.getW(), cutDataDO.getH()));
            return cutResult;
        }).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("data", cutResults);
        data.put("count", count);
        return data;
    }
}
