package org.river.file_server.entity;

import lombok.Data;

@Data
public class SystemInfo {
    private String operationSystemName;
    private Long totalSpace;
    private Long freeSpace;
    private Long usedSpace;
}
