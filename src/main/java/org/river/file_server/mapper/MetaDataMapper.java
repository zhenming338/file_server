package org.river.file_server.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.river.file_server.pojo.entity.Metadata;

@Mapper
public interface MetaDataMapper {
    void addMetaData(Metadata metaData);

    Metadata getMetaDataByFixedFileNamePrefix(String fixedNamePrefix);
}
