package org.river.file_server.controller;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.river.file_server.common.Result;
import org.river.file_server.mapper.MetaDataMapper;
import org.river.file_server.pojo.entity.MetaData;
import org.river.file_server.utils.FileHashCal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@RestController
@RequestMapping("file")
@RequiredArgsConstructor
public class FileController {

    public static final String FILE_NOTFOUND_ERROR = "file not found";

    public static final String DIRECTORY_NOTFOUND_ERROR = "directory not found";

    public static final String FILE_METADATA_ERROR = "file metadata read error";

    public static final String DIRECTORY_CREATE_FAILED = "create directory failed";

    public final MetaDataMapper metaDataMapper;

    @Value("${file.path}")
    private String baseFilePath;

    @Value("${file.host}")
    private String fileServerHost;

    @Value("${file.port}")
    private String fileServerPort;

    @GetMapping("/download/**")
    public void download(HttpServletRequest request, HttpServletResponse response) {
        judge();
        String uri = request.getRequestURI();
        String subFilePath = uri.substring(uri.indexOf("download/") + "download/".length());

        String fullFilePath = baseFilePath + subFilePath;

        if (subFilePath.isEmpty()) {
            try {
                response.getWriter().write("please input file path");
                return;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        String directoryPath = fullFilePath.substring(0, fullFilePath.lastIndexOf('/'));
        String fileName = fullFilePath.substring(fullFilePath.lastIndexOf('/') + 1);

//        String fileNamePrefix = fileName.substring(0, fileName.lastIndexOf('.'));
        MetaData metaData = metaDataMapper.getMetaDataByFixedFileNamePrefix(fileName);

        String encodedFileName = URLEncoder.encode(metaData.getOriginNamePrefix() + metaData.getNameSubfix(), StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
        response.setHeader("Content-Length", metaData.getSize().toString());
        if (fileName.isEmpty()) {
            try {
                response.getWriter().write("please input file name");
                return;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println(fileName);
        System.out.println(directoryPath);
        File file = new File(fullFilePath + metaData.getNameSubfix());
        if (!file.exists()) {
            try {
                PrintWriter writer = response.getWriter();
                writer.write(FILE_NOTFOUND_ERROR);
                return;
            } catch (IOException e) {
                System.out.println("cannot get outputStream");
            }
        }
        try (
                FileInputStream inputStream = new FileInputStream(file);
                ServletOutputStream outputStream = response.getOutputStream()) {
            byte[] buffer = new byte[10240];
            Integer len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }

            if (fileName.endsWith(".json")) {
                response.setContentType("application/json");
            } else {
                response.setContentType("application/octet-stream");
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @PostMapping("/upload/**")
    public Result<?> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws Exception {
        judge();

        String uri = request.getRequestURI();

        String sufDirectoryPath = uri.substring(uri.indexOf("upload/") + "upload/".length());
        String directoryPath = baseFilePath + sufDirectoryPath;
        System.out.println(directoryPath);
        File directory = new File(directoryPath);
        if (!directory.isDirectory()) {
            if (directory.mkdirs()) {
                System.out.println("create directory" + directoryPath);
            } else {
                System.out.println(baseFilePath + "create failed please check the authorized");
                return Result.error(DIRECTORY_CREATE_FAILED);
            }
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return Result.error(FILE_METADATA_ERROR);
        }

        String fileHash = FileHashCal.calculateHash(file, "MD5");
        MetaData metaDataByFixedFileNamePrefix = metaDataMapper.getMetaDataByFixedFileNamePrefix(fileHash);

        if (metaDataByFixedFileNamePrefix != null) {
            String fileLink = "http://"
                    + fileServerHost
                    + ":"
                    + fileServerPort
                    + File.separator
                    + "file/download"
                    + File.separator
                    + sufDirectoryPath
                    + File.separator
                    + metaDataByFixedFileNamePrefix.getFixedNamePrefix();
            return Result.success("upload success", fileLink);
        }
        MetaData metaData = new MetaData();

        String suffix = originalFilename.substring(originalFilename.lastIndexOf('.'));

        metaData.setOriginNamePrefix(originalFilename.substring(0, originalFilename.lastIndexOf('.')));
        metaData.setNameSubfix(suffix);
        metaData.setFixedNamePrefix(fileHash);
        metaData.setSize(file.getSize());
        metaData.setAddUser(request.getRemoteUser());
        metaData.setAddHost(request.getRemoteHost());
        metaData.setAddTime(LocalDateTime.now());
        String fixedFileName = metaData.getFixedNamePrefix() + suffix;
        System.out.println(fixedFileName);
        String fullFilePath = directoryPath + File.separator + fixedFileName;
        System.out.println(fullFilePath);
        System.out.println(metaData);
        System.out.println(request);


        metaDataMapper.addMetaData(metaData);
        try {
            file.transferTo(new File(fullFilePath));
            String fileLink = "http://"
                    + fileServerHost
                    + ":"
                    + fileServerPort
                    + File.separator
                    + "file/download"
                    + File.separator
                    + sufDirectoryPath
                    + File.separator
                    + metaData.getFixedNamePrefix();
            return Result.success("upload success", fileLink);
        } catch (IllegalStateException | IOException e) {
            return Result.error("file transform fail");
        }
    }

    private void judge() {
        File file = new File(baseFilePath);
        if (file.isDirectory()) {
            // System.out.println(baseFilePath+" is existed directory");
        } else {
            if (file.mkdirs()) {
                System.out.println(baseFilePath + " create success");
            } else {
                System.out.println(baseFilePath + "create failed please check the authorized");
            }
        }
    }
}
