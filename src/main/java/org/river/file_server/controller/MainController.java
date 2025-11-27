package org.river.file_server.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import org.river.file_server.common.Result;
import org.river.file_server.entity.FileInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api")
public class MainController {
    @Value("${file.path}")
    private String basePathStr;
    private final static Logger logger = LoggerFactory.getLogger(MainController.class);
    private static final String REQUEST_ILLEGAL_ERROR = "request path is illegal";
    @GetMapping("/getDirChildren")
    public Result<?> getDirectoryChildren(@RequestParam String path) throws IOException {
        logger.debug("get path : " + path);
        Path basePath = Path.of(basePathStr).toAbsolutePath();
        Path requPath = basePath.resolve(path).normalize().toAbsolutePath();
        logger.debug(basePathStr);
        logger.debug(requPath.toString());
        if (!requPath.startsWith(basePath)) {
            throw new RuntimeException(REQUEST_ILLEGAL_ERROR);
        }
        logger.debug("full path : " + requPath);
        File file = requPath.toFile();
        if (!file.exists()) {
            throw new RuntimeException(REQUEST_ILLEGAL_ERROR);
        }
        List<FileInfo> fileList = new ArrayList<>();
        String message = "";
        if (file.isDirectory()) {
            message = "this is a directory";
            File[] children = file.listFiles();
            Arrays.sort(Objects.requireNonNull(children), Comparator.comparing(f -> f.getName().toLowerCase()));
            for (int i = 0; i < Objects.requireNonNull(children).length; i++) {
                FileInfo fileInfo = new FileInfo();
                File child = children[i];
                fileInfo.setId((long) i);
                fileInfo.setName(child.getName());
                fileInfo.setIsFile(child.isFile());
                if (child.isFile()) {
                    fileInfo.setSize(child.length());
                }
                if (child.isDirectory()) {
                    fileInfo.setSize((long) Objects.requireNonNull(child.listFiles()).length);
                }
                BasicFileAttributes attr = Files.readAttributes(
                        Path.of(child.getAbsolutePath()),
                        BasicFileAttributes.class);
                ZonedDateTime createTimeZone = attr.creationTime().toInstant().atZone(ZoneId.systemDefault());
                ZonedDateTime updateTimeZone = attr.lastModifiedTime().toInstant().atZone(ZoneId.systemDefault());
                fileInfo.setCreateTime(createTimeZone.toLocalDateTime());
                fileInfo.setUpdateTime(updateTimeZone.toLocalDateTime());
                fileList.add(fileInfo);
            }
        }
        if (file.isFile()) {
            message = "this is a file";
        }
        return Result.success(message, fileList);
    }
}
