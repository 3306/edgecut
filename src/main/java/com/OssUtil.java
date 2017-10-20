package com;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.ObjectListing;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OssUtil {

//    @Value("${oss.endpoint}")
//    private String endpoint;

    private OSSClient ossClient;
    private String bucketName = "edgecut";

    public void init(){
        // endpoint以杭州为例，其它region请按实际情况填写
        String endpoint = "http://oss-cn-shanghai.aliyuncs.com";
        // 云账号AccessKey有所有API访问权限，建议遵循阿里云安全最佳实践，创建并使用RAM子账号进行API访问或日常运维，请登录 https://ram.console.aliyun.com 创建
        String accessKeyId = "";
        String accessKeySecret = "";
        // 创建OSSClient实例
        ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
    }

    public List<String> ls(String prefix){
        ObjectListing objectListing = ossClient.listObjects(bucketName, prefix);
        return objectListing.getObjectSummaries().stream().map(summary -> {
            return summary.getKey();
        }).collect(Collectors.toList());
    }

    public static void main(String[] args) {
        OssUtil ossUtil = new OssUtil();
        ossUtil.init();
        System.out.println(ossUtil.ls("1/"));
    }

}
