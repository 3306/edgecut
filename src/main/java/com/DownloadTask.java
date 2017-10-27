package com;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DownloadTask {
    private ZipOutputStream zipOutputStream;
    private AtomicInteger allCnt;
    private AtomicInteger finishCnt;
    private Boolean finish;
    private Boolean closed;

    public DownloadTask(String fileName) {
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
