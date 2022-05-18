package com.frag.taotie.service;

import com.frag.taotie.entity.DataDto;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ReportService {

    void report(DataDto dataDto);

    int reportMulTable(List<DataDto> list);

}
