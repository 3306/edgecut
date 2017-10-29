package com.edgecut.mapper;

import com.edgecut.entity.CutDataDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CutDataMapper {
    List<CutDataDO> query(CutDataDO cutDataDO);
}
