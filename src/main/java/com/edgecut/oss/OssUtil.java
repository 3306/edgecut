package com.edgecut.oss;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
public class OssUtil {

    private String bucketName = "edgecut";
    private String endpoint = System.getProperty("endpoint");
    private String accessKeyId = System.getProperty("accessKeyId");
    private String accessKeySecret = System.getProperty("accessKeySecret");
    private OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);

    private Logger logger = LoggerFactory.getLogger(getClass());

    public String getEndpoint() {
        return endpoint;
    }

    public String getBucketName() {
        return bucketName;
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

    public ObjectMetadata getObject(String key, String style, File file){
        GetObjectRequest request = new GetObjectRequest(bucketName, key);
        request.setProcess(style);
        return ossClient.getObject(request, file);
    }
}
