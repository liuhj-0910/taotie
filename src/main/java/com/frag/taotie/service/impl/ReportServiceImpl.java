package com.frag.taotie.service.impl;

import com.frag.taotie.component.TaotieActuator;
import com.frag.taotie.dao.ReportMapper;
import com.frag.taotie.entity.DataDto;
import com.frag.taotie.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author liuhj
 */
@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private TaotieActuator<DataDto> dataSingleQueueProcessor;

    @Autowired
    private ReportMapper reportMapper;

    @Override
    public void report(DataDto dataDto) {
        dataSingleQueueProcessor.offer(dataDto);
    }

    @Override
    public int reportMulTable(List<DataDto> list) {
        int table = list.get(0).getUserId().intValue() & (dataSingleQueueProcessor.getQueueNum() - 1);
        return reportMapper.reportMulTable(list, table);
    }

}
