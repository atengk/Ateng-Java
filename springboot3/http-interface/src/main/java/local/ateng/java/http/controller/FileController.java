package local.ateng.java.http.controller;

import jakarta.servlet.http.HttpServletResponse;
import local.ateng.java.http.entity.MyUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/file")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    // 上传单个文件
    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file) {
        // 获取文件名
        String fileName = file.getOriginalFilename();

        return "File uploaded successfully: " + fileName;
    }

    // 上传文件和其他字段（例如描述）
    @PostMapping("/uploadWithFields")
    public String uploadFileWithFields(@RequestParam("file") MultipartFile file,
                                       @RequestParam("description") String description) {
        String fileName = file.getOriginalFilename();
        return "File " + fileName + " uploaded with description: " + description;
    }

    // 上传文件和实体类
    // https://www.cnblogs.com/Jason-Xiang/p/14611447.html
    @PostMapping("/uploadWithJson")
    public String uploadFileWithJson(@RequestPart("file") MultipartFile file,
                                     @RequestPart("user") MyUser user) {
        String fileName = file.getOriginalFilename();
        return "File " + fileName + " uploaded with user: " + user;
    }

    // 上传多个文件
    @PostMapping("/uploadMultiple")
    public String uploadMultipleFiles(@RequestParam("files") List<MultipartFile> files) {
        StringBuilder fileNames = new StringBuilder();

        // 遍历所有上传的文件
        for (MultipartFile file : files) {
            fileNames.append(file.getOriginalFilename()).append(", ");
        }

        return "Uploaded files: " + fileNames;
    }

    /**
     * 导出CSV数据接口
     *
     * 流式生成包含10万条记录的CSV文件，支持断点续传和客户端中断检测
     *
     * @param response HttpServletResponse对象，用于设置响应头和获取输出流
     */
    @GetMapping("/export-csv")
    public void exportCsvData(HttpServletResponse response) {
        // 设置响应类型为CSV文件下载
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=csv_export_" + System.currentTimeMillis() + ".csv");

        // 创建流式响应体
        StreamingResponseBody stream = outputStream -> {
            try {
                // 写入CSV文件头
                String header = "ID,名称,描述\n";
                outputStream.write(header.getBytes(StandardCharsets.UTF_8));

                // 分批生成数据并写入输出流
                for (int i = 1; i <= 1000000; i++) {
                    String row = i + ",名称" + i + ",描述" + i + "\n";
                    try {
                        outputStream.write(row.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        handleIOException(e);
                        break; // 发生写入异常时终止循环
                    }

                    // 每1000条刷新一次缓冲区
                    if (i % 1000 == 0) {
                        outputStream.flush();
                    }
                }

                // 最终刷新确保所有数据写入
                outputStream.flush();
            } catch (IOException e) {
                handleIOException(e);
            }
        };

        try {
            // 将流式响应写入响应输出流
            OutputStream os = response.getOutputStream();
            stream.writeTo(os);
        } catch (Exception e) {
            // 记录日志但不抛出异常，避免重复报错
            System.err.println("CSV导出过程中发生异常：" + e.getMessage());
        }
    }

    /**
     * 统一处理IO异常
     *
     * @param e 发生的IO异常对象
     */
    private void handleIOException(IOException e) {
        if (isClientAbortException(e)) {
            System.out.println("客户端已主动断开连接，终止数据导出");
        } else {
            System.err.println("CSV导出过程中发生IO异常：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 判断是否是客户端主动断开连接异常
     *
     * @param e 需要判断的异常对象
     * @return 如果是客户端断开异常返回true，否则返回false
     */
    private boolean isClientAbortException(IOException e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause.getClass().getSimpleName().contains("ClientAbortException")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
