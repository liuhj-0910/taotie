package com.frag.taotie.config;

import com.frag.taotie.component.TaotieActuator;
import com.frag.taotie.entity.DataDto;
import com.frag.taotie.service.ReportService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author liuhj
 */
@Configuration
public class ProcessorConfiguration {

    @Value("${taotie.queuePower:6}")
    private int queuePower;

    @Value("${taotie.pollThreadPower:1}")
    private int pollThreadPower;

    @Value("${taotie.benchCommitNum:4000}")
    private int benchCommitNum;

    @Value("${taotie.commitThreadNum:10}")
    private int commitThreadNum;

    @Value("${taotie.queueCapacityFactor:1.0}")
    private float queueCapacityFactor;

    @Value("${taotie.offerTimeout:10}")
    private int offerTimeout;

    @Value("${taotie.pollTimeout:100}")
    private int pollTimeout;

    @Bean
    public TaotieActuator<DataDto> dataReportProcessor(ReportService reportService) {
//        return TaotieActuator.multipleQueueProcessor(queuePower, benchCommitNum, pollThreadPower, commitThreadNum, queueCapacityFactor, reportService::reportMulTable, DataDto::getUserId);
//        return TaotieActuator.singleQueueProcessor(benchCommitNum, commitThreadNum, offerTimeout, pollTimeout, reportMapper::report);
        return new TaotieActuator<DataDto>(queuePower, benchCommitNum, pollThreadPower, commitThreadNum, queueCapacityFactor, offerTimeout, pollTimeout,
                reportService::reportMulTable, DataDto::getUserId);

    }
}
