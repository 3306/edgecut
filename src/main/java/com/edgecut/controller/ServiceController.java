package com.edgecut.controller;

import com.edgecut.oss.CutResult;
import com.edgecut.oss.DownloadTask;
import com.edgecut.oss.OssUtil;
import com.edgecut.service.EdgeCutService;
import com.edgecut.service.StsService;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyuncs.sts.model.v20150401.AssumeRoleResponse;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/service")
public class ServiceController {
    @Resource
    private EdgeCutService edgeCutService;
    @Resource
    private OssUtil ossUtil;
    @Resource
    private StsService stsService;

    private String ossUrl = "http://edgecut.oss-cn-shanghai.aliyuncs.com/";

    private Map<String, DownloadTask> downloadTaskMap = new ConcurrentHashMap<>();

    private Cache<String, DownloadTask> downloadCache = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES).build();

    @RequestMapping("/run")
    @ResponseBody
    public int batchRun(@RequestParam("prefix")String prefix){
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

    @RequestMapping("/reject")
    @ResponseBody
    public void reject(@RequestParam("key") String key){
        ossUtil.addMetaData(key, "cutstatus", "-1");
    }

    @RequestMapping("/update")
    @ResponseBody
    public void update(@RequestParam("key") String key,
                       @RequestParam("x") String x,
                       @RequestParam("y") String y,
                       @RequestParam("w") String w,
                       @RequestParam("h") String h){
        String edge = String.format("%d/%d/%d/%d", Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(w), Integer.valueOf(h));
        ossUtil.addMetaData(key, "edge", edge);
        ossUtil.addMetaData(key, "cutstatus", "1");
    }

    @RequestMapping("/result")
    @ResponseBody
    public Map<String, Object> getResult(@RequestParam("prefix")String prefixInput,
                                         @RequestParam(required = false, value = "next") String next,
                                         @RequestParam("count") String count){
        final String prefix = prefixInput.endsWith("/") ? prefixInput : prefixInput + "/";
        ObjectListing ls = ossUtil.ls(prefix, next, Integer.valueOf(count));
        Map<String, Object> data = new HashMap<>();
        List<CutResult> edgeResults = ls.getObjectSummaries().stream().filter(ossObjectSummary -> !ossObjectSummary.getKey().equals(prefix)).map(ossObjectSummary -> {
            CutResult cutResult = new CutResult(ossObjectSummary.getKey());
            cutResult.setOriginDownloadUrl(ossUrl + ossObjectSummary.getKey());
            cutResult.setOriginShowUrl(ossUrl + ossObjectSummary.getKey() + "?x-oss-process=image/format,webp/resize,w_800");
            ObjectMetadata metaData = ossUtil.getMetaData(ossObjectSummary.getKey());
            String edge = metaData.getUserMetadata().get("edge");
            if (StringUtils.isNotBlank(edge)) {
                String[] split = edge.split("/");
                if (split.length == 4) {
                    int x = Integer.valueOf(split[0]);
                    int y = Integer.valueOf(split[1]);
                    int w = Integer.valueOf(split[2]);
                    int h = Integer.valueOf(split[3]);
                    cutResult.setX(x);
                    cutResult.setY(y);
                    cutResult.setW(w);
                    cutResult.setH(h);
                    cutResult.setCutShowUrl(ossUrl + ossObjectSummary.getKey() + String.format("?x-oss-process=image/crop,x_%d,y_%d,w_%d,h_%d/format,webp/resize,w_800", x, y, w, h));
                    cutResult.setCutDownloadUrl(ossUrl + ossObjectSummary.getKey() + String.format("?x-oss-process=image/crop,x_%d,y_%d,w_%d,h_%d", x, y, w, h));
                }
            }
            cutResult.setStatus(metaData.getUserMetadata().get("cutstatus"));
            return cutResult;
        }).collect(Collectors.toList());
        data.put("data", edgeResults);
        if (ls.isTruncated()) {
            data.put("next", ls.getNextMarker());
        }
        return data;
    }
}
