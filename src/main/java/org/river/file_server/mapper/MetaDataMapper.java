package org.river.file_server.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.river.file_server.pojo.entity.MetaData;

@Mapper
public interface MetaDataMapper {
    void addMetaData(MetaData metaData);

    MetaData getMetaDataByFixedFileNamePrefix(String fixedNamePrefix);
}
