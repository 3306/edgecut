package com.edgecut.oss;

import org.springframework.beans.BeanUtils;

import java.io.*;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DownloadTask {

    private Date startTime;
    private ZipOutputStream zipOutputStream;
    private AtomicInteger allCnt;
    private AtomicInteger finishCnt;
    //任务生产结束，allCnt不再增长
    private Boolean finish;
    //任务消费结束，打包完成，可以下载
    private Boolean closed;
    private String targetUrl;

    public DownloadTask() {

    }

    public DownloadTask(String fileName) {
        startTime = new Date();
        try {
            this.zipOutputStream = new ZipOutputStream(new FileOutputStream(fileName));
        } catch (FileNotFoundException e) {
            throw new RuntimeException();
        }
        allCnt = new AtomicInteger();
        finishCnt = new AtomicInteger();
        finish = false;
        closed = false;
    }

    public DownloadTask toOutput(){
        DownloadTask that = new DownloadTask();
        BeanUtils.copyProperties(this, that);
        that.setZipOutputStream(null);
        return that;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public ZipOutputStream getZipOutputStream() {
        return zipOutputStream;
    }

    public void setZipOutputStream(ZipOutputStream zipOutputStream) {
        this.zipOutputStream = zipOutputStream;
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

    public Boolean getClosed() {
        return closed;
    }

    public void setClosed(Boolean closed) {
        this.closed = closed;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public synchronized void nextEntry(String fileName, File file) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))){
            zipOutputStream.putNextEntry(new ZipEntry(fileName));
            byte[] tmp = new byte[102400];
            int t;
            while ((t = bis.read(tmp)) != -1) {
                zipOutputStream.write(tmp, 0, t);
            }
            zipOutputStream.closeEntry();
        }
    }

    public void tryClose(String key) {
        if (closed){
            return;
        }

        int fc = key == null ? finishCnt.get() : finishCnt.incrementAndGet();

        if (finish) {
            synchronized (this) {
                if (finish && !closed && allCnt.get() <= fc) {
                    try {
                        zipOutputStream.close();
                    } catch (IOException e) {
                        throw new RuntimeException();
                    } finally {
                        closed = true;
                    }
                }
            }
        }
    }
}
