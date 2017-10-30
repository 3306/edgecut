package com.edgecut.mapper;

import com.edgecut.entity.CutDataCountDO;
import com.edgecut.entity.CutDataDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CutDataMapper {
    List<CutDataDO> query(CutDataDO cutDataDO);
    List<CutDataCountDO> countStatus(CutDataDO cutDataDO);
    void save(CutDataDO cutDataDO);
    void insertOrCancel(CutDataDO cutDataDO);
}
