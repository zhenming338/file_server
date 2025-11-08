package org.river.file_server.Utils;

import org.river.file_server.entity.SystemInfo;

import java.io.File;
import java.util.Properties;

public class SystemUtil {
    public static SystemInfo getSystemInfo(){
        SystemInfo systemInfo = new SystemInfo();
        systemInfo.setOperationSystemName(System.getProperty("java.vendor"));

        File file = new File("/");
        systemInfo.setTotalSpace(file.getTotalSpace());
        systemInfo.setUsedSpace(file.getTotalSpace()-file.getUsableSpace());
        systemInfo.setFreeSpace(file.getFreeSpace());
        return systemInfo;
    }

    public static void main(String[] args) {
        Properties properties = System.getProperties();
        properties.forEach((key,value)->{
            System.out.println(key+" : "+value);
        });
    }
}
