package com.edgecut.entity;

import java.util.Date;

public class CutDataDO {
    private Long id;
    private String key;
    private String prefix;
    private Integer status;
    private Integer x,y,w,h;
    private Date gmtCreate;
    private Date gmtModified;
    private Integer deleteMark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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

    public Date getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public Date getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }

    public Integer getDeleteMark() {
        return deleteMark;
    }

    public void setDeleteMark(Integer deleteMark) {
        this.deleteMark = deleteMark;
    }

    @Override
    public String toString() {
        return "CutDataDO{" +
                "id=" + id +
                ", key='" + key + '\'' +
                ", prefix='" + prefix + '\'' +
                ", status=" + status +
                ", x=" + x +
                ", y=" + y +
                ", w=" + w +
                ", h=" + h +
                ", gmtCreate=" + gmtCreate +
                ", gmtModified=" + gmtModified +
                ", deleteMark=" + deleteMark +
                '}';
    }
}
