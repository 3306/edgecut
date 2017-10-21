package com.controller;

import com.CutResult;
import com.EdgeCutService;
import com.OssUtil;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.ObjectMetadata;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
public class ServiceController {
    @Resource
    private EdgeCutService edgeCutService;
    @Resource
    private OssUtil ossUtil;

    private String ossUrl = "http://edgecut.oss-cn-shanghai.aliyuncs.com/";

    @RequestMapping("/service/run")
    @ResponseBody
    public int batchRun(@RequestParam("prefix")String prefix){
        if (!prefix.endsWith("/")){
            prefix = prefix + "/";
        }
        return edgeCutService.batchRun(prefix);
    }

    @RequestMapping("/service/baseDir")
    @ResponseBody
    public List<String> baseDir(){
        return ossUtil.getBaseDir();
    }

    @RequestMapping("/service/result")
    @ResponseBody
    public Map<String, Object> getResult(@RequestParam("prefix")String prefixInput,
                                         @RequestParam(required = false, value = "next") String next,
                                         @RequestParam("count") String count){
        final String prefix = prefixInput.endsWith("/") ? prefixInput : prefixInput + "/";
        ObjectListing ls = ossUtil.ls(prefix, next, Integer.valueOf(count));
        Map<String, Object> data = new HashMap<>();
        List<CutResult> edgeResults = ls.getObjectSummaries().stream().filter(ossObjectSummary -> !ossObjectSummary.getKey().equals(prefix)).map(ossObjectSummary -> {
            CutResult cutResult = new CutResult();
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
                    int d = Integer.valueOf(split[3]);
                    cutResult.setX(x);
                    cutResult.setY(y);
                    cutResult.setW(w);
                    cutResult.setD(d);
                    cutResult.setCutShowUrl(ossUrl + ossObjectSummary.getKey() + String.format("?x-oss-process=image/crop,x_%d,y_%d,w_%d,h_%d/format,webp/resize,w_800", x, y, w, d));
                    cutResult.setCutDownloadUrl(ossUrl + ossObjectSummary.getKey() + String.format("?x-oss-process=image/crop,x_%d,y_%d,w_%d,h_%d", x, y, w, d));
                }
            }
            return cutResult;
        }).collect(Collectors.toList());
        data.put("data", edgeResults);
        if (ls.isTruncated()) {
            data.put("next", ls.getNextMarker());
        }
        return data;
    }
}
