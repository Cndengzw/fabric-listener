package com.fabric.display.controller;

import com.fabric.display.bean.*;
import com.fabric.display.bean.vo.MonthCountVO;
import com.fabric.display.bean.vo.TransactionVO;
import com.fabric.display.service.BlockInfoService;
import com.fabric.display.service.ListennerService;
import com.fabric.display.service.SQLTransactionService;

import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;

import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.sdk.BlockEvent;

import org.hyperledger.fabric.sdk.Channel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @Author Deng Zhiwen
 * @Date 2020/7/7 13:56
 */
@Api(tags = "fabric-display", description = "首页展示区块交易信息")
@RestController
@RequestMapping("fabric/display")
@Slf4j
public class DisplayController {

    @Autowired
    private ListennerService listenerService;
    @Autowired
    private SQLTransactionService sqlTransactionService;

    @ApiOperation("获得最新 N 个区块信息(默认是最新的一个)")
    @GetMapping("getNewestBlockInfo")
    public String getNewestBlockInfo(@RequestParam("channelName") String channelName, @RequestParam(value = "count", required = false) Integer count) throws IOException, ParseException {
        count = count == null ? 1 : count;
        return listenerService.getLastBlockInfo(channelName, count);
    }

    @ApiOperation("获得最新 N 个交易(默认是最新的5个)")
    @GetMapping("getNewestTransactions")
    public Set<TransactionVO> getNewestTransactions(@RequestParam(value = "count", required = false) Integer count) {
        count = count == null ? 5 : count;
        return listenerService.selectNewestTransactions(count);
    }

    @ApiOperation("查询最近12个月交易趋势")
    @GetMapping("selectLastOneYearTransactions")
    public MonthCountVO[] selectLastOneYearTransactions() {
        Map<Integer, Integer> resultMap = sqlTransactionService.selectLastOneYearTransactions();
        MonthCountVO[] result = new MonthCountVO[12];
        int i = 0;

        for (Map.Entry<Integer, Integer> integerIntegerEntry : resultMap.entrySet()) {
            MonthCountVO monthCountVO = new MonthCountVO();
            monthCountVO.setMonth(integerIntegerEntry.getKey());
            monthCountVO.setCount(integerIntegerEntry.getValue());
            result[i++] = monthCountVO;
        }
        return result;
    }

    @GetMapping("selectOneYearTransactions")
    @ApiOperation("查询年交易趋势")
    public Map<Integer, Integer> selectOneYearTransactions(@RequestParam("year") Integer year) {
        return sqlTransactionService.selectOneYearTransactions(year);
    }

    @GetMapping("selectOneMonthTransactions")
    @ApiOperation("查询月交易趋势")
    public Map<Integer, Integer> selectOneMonthTransactions(@RequestParam("year") Integer year, @RequestParam("month") Integer month) {
        return sqlTransactionService.selectOneMonthTransactions("foochannel", year, month);
    }


}
