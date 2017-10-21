package com;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OssUtil implements InitializingBean{

    private OSSClient ossClient;
    private String bucketName = "edgecut";

    private Logger logger = LoggerFactory.getLogger(getClass());

    public void init(){
        String endpoint = System.getProperty("endpoint");
        String accessKeyId = System.getProperty("accessKeyId");
        String accessKeySecret = System.getProperty("accessKeySecret");
        ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
    }

    public ObjectListing ls(String prefix, String nextMarker, int maxKeys){
        ListObjectsRequest request = new ListObjectsRequest(bucketName)
                .withPrefix(prefix).withMarker(nextMarker).withMaxKeys(maxKeys);
        return ossClient.listObjects(request);
    }

    public ObjectMetadata getMetaData(String key){
        return ossClient.getObjectMetadata(bucketName, key);
    }

    public void addMetaData(String key, String k, String v){
        ObjectMetadata objectMetadata = getMetaData(key);
        objectMetadata.addUserMetadata(k, v);

        CopyObjectRequest copyObjectRequest = new CopyObjectRequest(bucketName, key, bucketName, key);
        copyObjectRequest.setNewObjectMetadata(objectMetadata);

        CopyObjectResult copyObjectResult = ossClient.copyObject(copyObjectRequest);
        logger.info("addMetaData key={} k={} v={} etag={} lastModified={}", key, k, v, copyObjectResult.getETag(), copyObjectResult.getLastModified());
    }

    public List<String> getBaseDir(){
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName);
        listObjectsRequest.setDelimiter("/");
        ObjectListing listing = ossClient.listObjects(listObjectsRequest);
        return listing.getCommonPrefixes();
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }
}
