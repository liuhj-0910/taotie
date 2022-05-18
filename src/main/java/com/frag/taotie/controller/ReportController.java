package com.frag.taotie.controller;

import com.frag.taotie.entity.DataDto;
import com.frag.taotie.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author liuhj
 */
@Slf4j
@RestController
public class ReportController {

    @Autowired
    private ReportService reportService;

    @RequestMapping("/report")
    public void report(@RequestParam Long userId, @RequestParam String msg) {
        long start = System.currentTimeMillis();
        DataDto dataDto = new DataDto();
        dataDto.setUserId(userId);
        dataDto.setMsg(msg);
        reportService.report(dataDto);
        log.trace("{}, 耗时{}毫秒", dataDto, System.currentTimeMillis() - start);
    }

}
