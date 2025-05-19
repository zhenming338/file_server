package org.river.file_server.pojo.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class MetaData {

    Long id;
    String originNamePrefix;
    String fixedNamePrefix;
    String nameSubfix;
    Long size;
    LocalDateTime addTime;
    String addUser;
    String addHost;
}
