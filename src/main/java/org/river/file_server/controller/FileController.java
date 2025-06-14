package org.river.file_server.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import jakarta.servlet.ServletOutputStream;
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

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Value("${file.path}")
    private String baseFilePathStr;

    @Value("${file.host}")
    private String fileServerHost;

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

        try (
                FileInputStream inputStream = new FileInputStream(file);
                ServletOutputStream outputStream = response.getOutputStream()) {
            response.setContentLengthLong(file.length());
            response.setHeader("Content-Disposition", "inline; filename=" + file.getName());
            response.setContentType("application/octet-stream");
            if (fileName.endsWith(".json")) {
                response.setContentType("application/json");
            }
            byte[] buffer = new byte[10240];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
        } catch (IOException e) {
            throw new RuntimeException("write to file error");
        }

    }

    @PostMapping("/upload/**")
    public Result<?> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        judge();
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
        baseFilePath = Paths.get(baseFilePathStr).toAbsolutePath().normalize();
    }
}
