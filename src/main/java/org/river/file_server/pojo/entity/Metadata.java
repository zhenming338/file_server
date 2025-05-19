package org.river.file_server.pojo.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Metadata {
    Long id;
    String namePrefix;
    String nameSubfix;
    String hash;
    Long size;
    LocalDateTime addTime;
    String addUser;
    String addHost;
}
