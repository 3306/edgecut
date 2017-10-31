package com.edgecut.oss;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class CutTask {

    private String prefix;
    private Date startTime;
    private AtomicInteger allCnt;
    private AtomicInteger finishCnt;
    //任务生产结束，
    private Boolean finish;
    //任务消费结束，
    private Boolean over;

    public CutTask() {
    }

    public CutTask(String prefix) {
        this.prefix = prefix;
        startTime = new Date();
        allCnt = new AtomicInteger();
        finishCnt = new AtomicInteger();
        finish = false;
        over = false;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
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
