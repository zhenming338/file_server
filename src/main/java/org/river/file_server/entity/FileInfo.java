package org.river.file_server.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class FileInfo {
    Long id;
    String name;
    Long size;
    Boolean isFile;
    LocalDateTime createTime;
    LocalDateTime updateTime;
}
