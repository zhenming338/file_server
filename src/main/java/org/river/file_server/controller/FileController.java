package org.river.file_server.controller;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.river.file_server.common.Result;
import org.river.file_server.mapper.MetaDataMapper;
import org.river.file_server.pojo.entity.Metadata;
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

    @Value("${file.hashType}")
    private String hashType;

    @GetMapping("/download/{fileHash}")
    public void download(HttpServletResponse response, @PathVariable("fileHash") String fileHash) {
        judge();

        Metadata metaData = metaDataMapper.getMetaDataByFixedFileNamePrefix(fileHash);

        if (metaData == null) {
            try {
                PrintWriter writer = response.getWriter();
                writer.write(FILE_NOTFOUND_ERROR);
                return;
            } catch (IOException e) {
                System.out.println("cannot get outputStream");
                return;

            }
        }

        String encodedFileName = URLEncoder.encode(metaData.getNamePrefix() + metaData.getNameSubfix(), StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
        response.setHeader("Content-Length", metaData.getSize().toString());

        File file = new File(baseFilePath + File.separator + metaData.getHash() + metaData.getNameSubfix());

        if (!file.exists()) {

            try {
                PrintWriter writer = response.getWriter();
                writer.write(FILE_NOTFOUND_ERROR);
                writer.close();
                return;
            } catch (IOException e) {
                System.out.println("cannot get outputStream");
                return;
            }
        }
        try (
                FileInputStream inputStream = new FileInputStream(file);
                ServletOutputStream outputStream = response.getOutputStream()) {
            byte[] buffer = new byte[10240];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }

            if (encodedFileName.endsWith(".json")) {
                response.setContentType("application/json");
            } else {
                response.setContentType("application/octet-stream");
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @PostMapping("/upload")
    public Result<?> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws Exception {

        judge();


        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return Result.error(FILE_METADATA_ERROR);
        }

        String fileHash = FileHashCal.calculateHash(file, hashType);
        Metadata metaData = metaDataMapper.getMetaDataByFixedFileNamePrefix(fileHash);

        if (metaData == null) {
            metaData = new Metadata();

            String subfix = originalFilename.substring(originalFilename.lastIndexOf('.'));

            metaData.setNamePrefix(originalFilename.substring(0, originalFilename.lastIndexOf('.')));
            metaData.setNameSubfix(subfix);
            metaData.setHash(fileHash);
            metaData.setSize(file.getSize());
            metaData.setAddUser(request.getRemoteUser());
            metaData.setAddHost(request.getRemoteHost());
            metaData.setAddTime(LocalDateTime.now());
            metaDataMapper.addMetaData(metaData);

        }

        String fixedFileName = metaData.getHash() + metaData.getNameSubfix();
        String fullFilePath = baseFilePath + File.separator + fixedFileName;
        try {
            file.transferTo(new File(fullFilePath));
            String fileLink = "http://"
                    + fileServerHost
                    + ":"
                    + fileServerPort
                    + File.separator
                    + "file/download"
                    + File.separator
                    + metaData.getHash();
            return Result.success("upload success", fileLink);
        } catch (IllegalStateException | IOException e) {
            return Result.error("file transform fail");
        }
    }

    private void judge() {
        File file = new File(baseFilePath);
        if (file.isDirectory()) {
            System.out.println("test");
        } else {
            if (file.mkdirs()) {
                System.out.println(baseFilePath + " create success");
            } else {
                System.out.println(baseFilePath + "create failed please check the authorized");
            }
        }
    }
}
