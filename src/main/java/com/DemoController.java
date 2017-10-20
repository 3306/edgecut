package com;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Controller
public class DemoController {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private String executor;

    private ThreadPoolExecutor pool = new ThreadPoolExecutor(10, 10, 1, TimeUnit.MINUTES, new LinkedBlockingQueue<>(1));

    @RequestMapping("/upload")
    public String uploadHtml(){
        return "uploadFile";
    }

    @RequestMapping("/test")
    public void test(HttpServletRequest request, HttpServletResponse response, @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        logger.info("getFile {}", file.getOriginalFilename());
        File file1 = new File(System.getProperty("java.io.tmpdir") + UUID.randomUUID());
//        file1.createNewFile();
        FileOutputStream fileOutputStream = new FileOutputStream(file1);
        fileOutputStream.write(file.getBytes());
        fileOutputStream.flush();
        fileOutputStream.close();
        String fileName = work(file1.getAbsolutePath());
        Integer index = executor.lastIndexOf("/");
        String outputFileStr = executor.substring(0, index + 1) + fileName;

        byte[] bytes = new byte[102400];
        int t = 0;
        ServletOutputStream outputStream = response.getOutputStream();
        try (FileInputStream result = new FileInputStream(outputFileStr)){
            while((t = result.read(bytes)) != -1){
                outputStream.write(bytes, 0, t);
            }
        } finally {
            outputStream.close();
        }
    }

    public String work(String inputFile) throws IOException {
        String commandStr = executor + " " + inputFile;
        logger.info("commandStr : {}", commandStr);
        Process p = Runtime.getRuntime().exec(commandStr);
        Scanner scanner = new Scanner(p.getInputStream());
        String fileName = null;
        while(scanner.hasNext()){
            if (fileName == null){
                fileName = scanner.nextLine();
                logger.info("command output: {}", fileName);
            } else {
                logger.info("command output: {}", scanner.nextLine());
            }
        }
        return fileName;
    }
}
