package com.edgecut.mapper;

import com.edgecut.entity.CutDataCountDO;
import com.edgecut.entity.CutDataDO;
import com.edgecut.entity.CutDataQTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CutDataMapper {
    List<CutDataDO> query(CutDataQTO cutDataQTO);
    Integer count(CutDataQTO cutDataQTO);
    void save(CutDataDO cutDataDO);
    void insertOrCancel(CutDataDO cutDataDO);
}
