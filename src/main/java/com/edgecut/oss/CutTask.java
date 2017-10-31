package com.edgecut.oss;

import com.edgecut.entity.CutDataDO;
import org.springframework.beans.BeanUtils;

import java.io.*;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CutTask {

    private Date startTime;
    private AtomicInteger allCnt;
    private AtomicInteger finishCnt;
    //任务生产结束，allCnt不再增长
    private Boolean finish;
    //任务消费结束，打包完成，可以下载
    private Boolean over;

    public CutTask(String fileName) {
        startTime = new Date();
        allCnt = new AtomicInteger();
        finishCnt = new AtomicInteger();
        finish = false;
        over = false;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public AtomicInteger getAllCnt() {
        return allCnt;
    }

    public void setAllCnt(AtomicInteger allCnt) {
        this.allCnt = allCnt;
    }

    public AtomicInteger getFinishCnt() {
        return finishCnt;
    }

    public void setFinishCnt(AtomicInteger finishCnt) {
        this.finishCnt = finishCnt;
    }

    public Boolean getFinish() {
        return finish;
    }

    public void setFinish(Boolean finish) {
        this.finish = finish;
    }

    public Boolean getOver() {
        return over;
    }

    public void setOver(Boolean over) {
        this.over = over;
    }
}
