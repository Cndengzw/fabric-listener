package com.fabric.display.service;

import com.fabric.display.bean.SQLTransaction;
import com.fabric.display.mapper.SQLTransactionMapper;
import com.google.common.collect.ImmutableMap;
import io.swagger.models.auth.In;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

import java.time.LocalDateTime;
import java.util.*;

/**
 * @Author Deng Zhiwen
 * @Date 2020/7/7 11:30
 */
@Service
@EnableScheduling
public class SQLTransactionService {

    @Autowired
    private SQLTransactionMapper sqlTransactionMapper;

    // 查询最近 12 个月的交易量
    public Map<Integer, Integer> selectLastOneYearTransactions() {
        int thisMonth = LocalDateTime.now().getMonthValue();
        int thisYear = LocalDateTime.now().getYear();
        int queryMonth = thisMonth + 1; // 去年这个月
        int queryYear = thisYear - 1;

        Map<Integer, Integer> lastOneYearTransactions = new LinkedHashMap<>();
        for (int i = 0; i < 11; i++) {
            if (queryMonth > 12) {
                queryMonth = 1;
                queryYear++;
            }
            SQLTransaction sqlTransaction = new SQLTransaction();
            sqlTransaction.setYear(queryYear);
            sqlTransaction.setMonth(queryMonth);
            if (queryMonth != LocalDateTime.now().getMonthValue()){
                sqlTransaction.setTimeType("month");
            }
            lastOneYearTransactions.put(queryMonth, sqlTransactionMapper.select(sqlTransaction).size());
            queryMonth++;
        }

        return lastOneYearTransactions;
    }

    // 查找某一年的每月交易量
    public Map<Integer, Integer> selectOneYearTransactions(int year) {
        SQLTransaction sqlTransaction = new SQLTransaction();
        sqlTransaction.setYear(year);
        sqlTransaction.setTimeType("month");
        List<SQLTransaction> years = sqlTransactionMapper.select(sqlTransaction);

        HashMap<Integer, Integer> monthTransactionMap = new HashMap<>();
        for (SQLTransaction transaction : years) {
            monthTransactionMap.put(transaction.getMonth(), transaction.getCount());
        }

        // 当前月需要单独统计
        SQLTransaction thisMonthSqlTransaction = new SQLTransaction();
        thisMonthSqlTransaction.setYear(year);
        thisMonthSqlTransaction.setMonth(LocalDateTime.now().getMonthValue());
        // 这里用 通用mapper 真的不方便，我只想统计 count ，但是却不得不统计所有条目（或许有好的方法，但是不明显）
        List<SQLTransaction> select = sqlTransactionMapper.select(thisMonthSqlTransaction);
        int thisMonthCount = 0;
        for (SQLTransaction transaction : select) {
            thisMonthCount += transaction.getCount();
        }

        monthTransactionMap.put(LocalDateTime.now().getMonthValue(), thisMonthCount);

        return monthTransactionMap;
    }

    // 查找某一月的每日交易量
    public Map<Integer, Integer> selectOneMonthTransactions(String channelName, Integer year, Integer month) {
        SQLTransaction sqlTransaction = new SQLTransaction();
        sqlTransaction.setYear(year);
        sqlTransaction.setMonth(month);
        sqlTransaction.setTimeType("day");
        sqlTransaction.setChannelName(channelName);
        List<SQLTransaction> months = sqlTransactionMapper.select(sqlTransaction);

        HashMap<Integer, Integer> dayTransactionMap = new HashMap<>();
        for (SQLTransaction transaction : months) {
            dayTransactionMap.put(transaction.getDay(), transaction.getCount());
        }

        Integer thisDayCount = selectOneDayTransactions(channelName, year, month, LocalDateTime.now().getDayOfMonth());
        dayTransactionMap.put(LocalDateTime.now().getDayOfMonth(), thisDayCount);

        return dayTransactionMap;
    }

    // 查找某一天的所有交易量
    public Integer selectOneDayTransactions(String channelName, Integer year, Integer month, Integer day) {
        SQLTransaction sqlTransaction = new SQLTransaction();
        sqlTransaction.setYear(year);
        sqlTransaction.setMonth(month);
        sqlTransaction.setDay(day);
        sqlTransaction.setChannelName(channelName);
        return sqlTransactionMapper.select(sqlTransaction).size();
    }


    // 插入一条数据
    public void insertOneTransaction(SQLTransaction sqlTransaction) {
        sqlTransactionMapper.insert(sqlTransaction);
    }


}
