package com.frag.taotie.dao;

import com.frag.taotie.entity.DataDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author liuhj
 */
@Mapper
@Repository
public interface ReportMapper {

    int report(@Param("list") List<DataDto> list);

    int reportMulTable(@Param("list") List<DataDto> list, @Param("table") int table);

}
