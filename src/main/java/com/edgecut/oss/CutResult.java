package com.edgecut.oss;

public class CutResult {
    private String key;
    private String originDownloadUrl;
    private String originShowUrl;
    private String cutShowUrl;
    private String cutDownloadUrl;
    private Integer x,y,w,h;
    private Integer status;

    public CutResult(String key) {
        this.key = key;
    }

    public String getOriginDownloadUrl() {
        return originDownloadUrl;
    }

    public void setOriginDownloadUrl(String originDownloadUrl) {
        this.originDownloadUrl = originDownloadUrl;
    }

    public String getOriginShowUrl() {
        return originShowUrl;
    }

    public void setOriginShowUrl(String originShowUrl) {
        this.originShowUrl = originShowUrl;
    }

    public String getCutShowUrl() {
        return cutShowUrl;
    }

    public void setCutShowUrl(String cutShowUrl) {
        this.cutShowUrl = cutShowUrl;
    }

    public String getCutDownloadUrl() {
        return cutDownloadUrl;
    }

    public void setCutDownloadUrl(String cutDownloadUrl) {
        this.cutDownloadUrl = cutDownloadUrl;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }

    public Integer getW() {
        return w;
    }

    public void setW(Integer w) {
        this.w = w;
    }

    public Integer getH() {
        return h;
    }

    public void setH(Integer h) {
        this.h = h;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
