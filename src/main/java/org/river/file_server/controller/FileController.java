package org.river.file_server.controller;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.river.file_server.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("file")
@RequiredArgsConstructor
public class FileController {

    public static final String FILE_NOTFOUND_ERROR = "file not found";
    public static final String DIRECTORY_NOTFOUND_ERROR = "directory not found";
    public static final String FILE_METADATA_ERROR = "file metadata read error";
    public static final String DIRECTORY_CREATE_FAILED = "create directory failed";
    public static final String TOKEN_VAIL_FAILED = "token vail failed";
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Value("${file.path}")
    private String baseFilePathStr;

    @Value("${file.host}")
    private String fileServerHost;

    @Value("${file.tokenTarget}")
    private String tokenTarget;

    @Value("${file.port}")
    private String fileServerPort;

    private Path baseFilePath;

    @GetMapping("/download/**")
    public void download(HttpServletRequest request, HttpServletResponse response) {
        judge();
        String uri = request.getRequestURI();
        String uriFilePathStr = uri.substring(uri.indexOf("download/") + "download/".length());
        if (uriFilePathStr.isEmpty()) {
            try {
                response.getWriter().write("please input file path");
                return;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        uriFilePathStr = URLDecoder.decode(uriFilePathStr, StandardCharsets.UTF_8);
        Path fullFilePath = baseFilePath.resolve(uriFilePathStr).normalize();
        logger.debug(fullFilePath.toString());
        if (!fullFilePath.startsWith(baseFilePath)) {
            throw new RuntimeException("illegal routes request");
        }
        Path fileName = fullFilePath.getFileName();
        if (fileName == null || fileName.toString().isEmpty()) {
            throw new RuntimeException("missing file name");
        }
        File file = fullFilePath.toFile();
        if (!file.exists()) {
            throw new RuntimeException("file not found");
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "r");
             OutputStream out = response.getOutputStream()) {

            long fileLength = file.length();
            String range = request.getHeader("Range");
            response.setHeader("Accept-Ranges", "bytes");
            response.setContentType(Files.probeContentType(file.toPath()));
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + URLEncoder.encode(file.getName(), StandardCharsets.UTF_8) + "\"");
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
        } catch (IOException e) {
            throw new RuntimeException("write to file error");
        }

    }

    @PostMapping("/upload/**")
    public Result<?> upload(@RequestParam MultipartFile file, String token, String encode, HttpServletRequest request) {
        judge();
        if (token == null || token.isEmpty() || !token.equals(tokenTarget)) {
            throw new RuntimeException(TOKEN_VAIL_FAILED);
        }
        String uri = request.getRequestURI();

        String uriFilePathStr = uri.substring(uri.indexOf("upload/") + "upload/".length());
        Path fullDirPath = baseFilePath.resolve(uriFilePathStr).normalize();

        if (!fullDirPath.startsWith(baseFilePath)) {
            throw new RuntimeException("illegal router request");
        }
        logger.debug(fullDirPath.toString());
        File directory = fullDirPath.toFile();

        if (!directory.isDirectory()) {
            if (!directory.mkdirs()) {
                throw new RuntimeException("create directory fail");
            }
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new RuntimeException(FILE_METADATA_ERROR);
        }

        String suffix = originalFilename.substring(originalFilename.indexOf('.'));
        String fixedFileName = Math.abs(UUID.randomUUID().getLeastSignificantBits()) + suffix;

        Path fullFilePath = fullDirPath.resolve(fixedFileName).normalize();
        try {
            file.transferTo(fullFilePath.toFile());
            String fileLink = "http://"
                    + fileServerHost
                    + ":"
                    + fileServerPort
                    + File.separator
                    + "file/download"
                    + File.separator
                    + uriFilePathStr
                    + File.separator
                    + fixedFileName;
            return Result.success("upload success", fileLink);
        } catch (IllegalStateException | IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private void judge() {
        File file = new File(baseFilePathStr);
        if (!file.isDirectory()) {
            if (!file.mkdirs()) {
                throw new RuntimeException("create base directory failed");
            }
        }
        baseFilePath = Path.of(baseFilePathStr).toAbsolutePath().normalize();
    }
}
