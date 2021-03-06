package com.fabric.display.schedule;

import com.fabric.display.service.ListennerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * @author Deng Zhiwen
 * @date 2020/7/14 13:46
 */
@Configuration
@EnableScheduling
public class ScheduledTasks {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);

    @Autowired
    private ListennerService listennerService;


    // 每天 23:58 统计当天交易量
    @Async
    @Scheduled(cron = "0 58 23 * * ?")
    public void insertOneDayTransactions() {
        listennerService.insertOneDayTransactions();
    }

    // 每月1号 00:03 统计前一月交易量
    @Async
    @Scheduled(cron = "0 3 0 1 * ?")
    public void insertOneHourTransactions() {
        listennerService.insertOneMonthTransactions();
    }

}
