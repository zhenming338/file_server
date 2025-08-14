package org.river.file_server.controller;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author : river
 */
@Controller("test")
public class TestController {

    @Value("${file.path}")
    private String baseFolderPath;

    @GetMapping("/download")
    public void downloadFile(
            @RequestParam String filename,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        File file = new File(baseFolderPath + filename);
        if (!file.exists()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        long fileLength = file.length();
        String range = request.getHeader("Range");

        response.setHeader("Accept-Ranges", "bytes");
        response.setContentType(Files.probeContentType(file.toPath()));
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + URLEncoder.encode(file.getName(), StandardCharsets.UTF_8) + "\"");

        try (RandomAccessFile raf = new RandomAccessFile(file, "r");
                OutputStream out = response.getOutputStream()) {

            if (range != null && range.startsWith("bytes=")) {
                // 解析 Range 头
                String[] parts = range.replace("bytes=", "").split("-");
                long start = Long.parseLong(parts[0]);
                long end = parts.length > 1 && !parts[1].isEmpty() ? Long.parseLong(parts[1]) : fileLength - 1;
                long contentLength = end - start + 1;

                // 设置 206 部分内容响应头
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                response.setHeader("Content-Range", "bytes %d-%d/%d".formatted(start, end, fileLength));
                response.setHeader("Content-Length", String.valueOf(contentLength));

                raf.seek(start);
                byte[] buffer = new byte[8192];
                long remaining = contentLength;
                int len;
                while ((len = raf.read(buffer)) != -1 && remaining > 0) {
                    if (remaining < len) {
                        out.write(buffer, 0, (int) remaining);
                        break;
                    }
                    out.write(buffer, 0, len);
                    remaining -= len;
                }

            } else {
                // 常规下载
                response.setHeader("Content-Length", String.valueOf(fileLength));
                byte[] buffer = new byte[8192];
                int len;
                while ((len = raf.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            }

            out.flush();
        }
    }
}
