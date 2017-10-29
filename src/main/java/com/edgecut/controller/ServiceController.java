package com.edgecut.controller;

import com.aliyun.oss.model.OSSObjectSummary;
import com.edgecut.entity.CutDataDO;
import com.edgecut.mapper.CutDataMapper;
import com.edgecut.oss.CutResult;
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
import java.util.concurrent.ConcurrentHashMap;
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

    private Map<String, DownloadTask> downloadTaskMap = new ConcurrentHashMap<>();

    private Cache<String, DownloadTask> downloadCache = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES).build();

    @RequestMapping("/run")
    @ResponseBody
    public int batchRun(@RequestParam("prefix") String prefix){
        if (!prefix.endsWith("/")){
            prefix = prefix + "/";
        }
        return edgeCutService.batchRun(prefix);
    }

    @RequestMapping("/baseDir")
    @ResponseBody
    public List<String> baseDir(){
        return ossUtil.getBaseDir();
    }

    @RequestMapping("/dirCount")
    @ResponseBody
    public Map<String, AtomicInteger> dirCount(@RequestParam("prefix") String prefix){
        if (!prefix.endsWith("/")){
            prefix = prefix + "/";
        }
        CutDataDO queryDO = new CutDataDO();
        queryDO.setPrefix(prefix);
        List<CutDataDO> cutDataCountDOS = cutDataMapper.query(queryDO);
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
    public DownloadTask download(@RequestParam("prefix") String prefix){
        if (!prefix.endsWith("/")){
            prefix = prefix + "/";
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
                                         @RequestParam(required = false, value = "next") String next,
                                         @RequestParam("count") String count){
        final String prefix = prefixInput.endsWith("/") ? prefixInput : prefixInput + "/";
        CutDataDO queryDO = new CutDataDO();
        queryDO.setPrefix(prefix);
        List<CutDataDO> cutDataDOS = cutDataMapper.query(queryDO);
        List<CutResult> cutResults = cutDataDOS.parallelStream().map(cutDataDO -> {
            CutResult cutResult = new CutResult(cutDataDO.getKey());
            cutResult.setOriginDownloadUrl(ossUrl + cutDataDO.getKey());
            cutResult.setOriginShowUrl(ossUrl + cutDataDO.getKey() + "?x-oss-process=image/format,webp/resize,w_800");
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
        return data;
    }
}
