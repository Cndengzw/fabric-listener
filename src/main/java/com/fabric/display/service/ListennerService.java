package com.fabric.display.service;

import com.fabric.display.bean.*;
import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author Deng Zhiwen
 * @Date 2020/7/3 14:15
 */
@Service
@Slf4j
public class ListennerService {

    @Autowired
    private SQLTransactionService sqlTransactionService;
    @Autowired
    private BlockInfoService blockInfoService;
    @Autowired
    private Gson gson;


    List<DataTransaction> cacheList = Collections.synchronizedList(new ArrayList<>()); // 缓存（用于存放最新的交易）
    private static Map<String, BlockEvent> blockEventMap = new ConcurrentHashMap<>();

    private static int halfHourCount = 0;

    private long lastBlockNumber;

    // SDK，给channel增加一个区块监听
    public void addBlockListener(Channel channel) {
        log.info(channel.getName() + "addBlockListener");
        try {
            channel.registerBlockListener(blockEvent -> {
                String dataHash = Arrays.toString(blockEvent.getDataHash());
                log.info("addBlockListener" + channel.getName() + " 监听到最新的了！dataHash: " + dataHash + ", BlockNumber: " + blockEvent.getBlockNumber());
                // 监听逻辑
                try {
                    lastBlockNumber = blockEvent.getBlockNumber();
                    blockEventMap.put(channel.getName(), blockEvent);
                    listenNewestBlock(blockEvent);
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
            });
        } catch (InvalidArgumentException e) {
            e.printStackTrace();
        }
    }

    // 获取最新区块信息
    public String getNewestBlockInfo(String channelName) throws InvalidProtocolBufferException {
        BlockEvent blockEvent = blockEventMap.get(channelName); // 最新区块事件
        if (blockEvent == null) {
            return "通道名输入错误或该通道启动监听功能后还未监听到任何区块";
        }

        BlockHeader blockHeader = blockInfoService.getBlockHeader(blockEvent);
        BlockMataData blockMataData = blockInfoService.getBlockMetaData(blockEvent);
        BlockData blockData = blockInfoService.getBlockData(blockEvent);

        return gson.toJson(new Block(blockHeader, blockData, blockMataData));
    }

    // 监听到最新区块事件
    public void listenNewestBlock(BlockEvent blockEvent) throws InvalidProtocolBufferException {
        log.info("listenNewestBlock: 监听到新的区块了，BlockNumber = " + blockEvent.getBlockNumber());
        int envelopeCount = blockEvent.getTransactionCount();

        // 得到所有区块交易
        BlockData blockData = blockInfoService.getBlockData(blockEvent);
        Set<DataTransaction> allTransactions = blockData.getAllTransactions();
        log.debug("listenNewestBlock: allTransactions.size() = " + allTransactions.size());

        for (DataTransaction transaction : allTransactions) {
            if (!cacheList.contains(transaction)) {
                halfHourCount += envelopeCount;
                cacheList.add(transaction);
            }
        }
        log.debug("listenNewestBlock: cacheList.size() = " + cacheList.size());
        while (cacheList.size() > 300) {    // 缓存最多保存 300条最新交易
            cacheList.remove(0);
        }

        lastBlockNumber = blockEvent.getBlockNumber();
    }

    // 定时任务，半小时插入一条数据
    public void insertHalfHourTransactions() {
        LocalDateTime dateTime = LocalDateTime.now();

        SQLTransaction sqlTransaction = new SQLTransaction();

        sqlTransaction.setYear(dateTime.getYear());
        sqlTransaction.setMonth(dateTime.getMonthValue());
        sqlTransaction.setDay(dateTime.getDayOfMonth());
        sqlTransaction.setHour(dateTime.getHour());
        sqlTransaction.setMinute(dateTime.getMinute());
        sqlTransaction.setLastBlockNumber(lastBlockNumber);

        sqlTransaction.setCount(halfHourCount);
        halfHourCount = 0;
        sqlTransactionService.insertOneTransaction(sqlTransaction);
    }

    // 定时任务，一天结束后统计当日数据并插入
    public void insertOneDayTransactions() {
        SQLTransaction sqlTransaction = new SQLTransaction();

        LocalDateTime dateTime = LocalDateTime.now();

        sqlTransaction.setTimeType("day");
        sqlTransaction.setYear(dateTime.getYear());
        sqlTransaction.setMonth(dateTime.getMonthValue());
        sqlTransaction.setDay(dateTime.getDayOfMonth());

        int count = sqlTransactionService.selectOneDayTransactions(dateTime.getYear(), dateTime.getMonthValue(), dateTime.getDayOfMonth());
        sqlTransaction.setCount(count);
        sqlTransactionService.insertOneTransaction(sqlTransaction);
    }

    // 定时任务，一个月结束后统计当月数据并插入
    public void insertOneMonthTransactions() {
        SQLTransaction sqlTransaction = new SQLTransaction();

        LocalDateTime dateTime = LocalDateTime.now();

        sqlTransaction.setTimeType("month");
        sqlTransaction.setYear(dateTime.getYear());
        sqlTransaction.setMonth(dateTime.getMonthValue());

        int month = dateTime.getMonthValue() - 1;
        if (month == 0){
            // 该月是1月，上一个月应该为 12月
            month = 12;
        }

        Map<Integer, Integer> daysTransactions = sqlTransactionService.selectOneMonthTransactions(dateTime.getYear(), month);
        int count = 0;

        Collection<Integer> daysTransactionsCount = daysTransactions.values();
        for (Integer daysTransaction : daysTransactionsCount) {
            count += daysTransaction;
        }
        sqlTransaction.setCount(count);
        sqlTransactionService.insertOneTransaction(sqlTransaction);
    }


    // 查询最新交易动态
    public Set<DataTransaction> selectNewestTransactions(Integer number) {
        log.debug("selectNewestTransactions : cacheList.size() = " + cacheList.size());
        Set<DataTransaction> set = new LinkedHashSet<>();
        number = number > cacheList.size()? cacheList.size(): number;
        for (int i = 0; i < number; i++) {
            int lastIndex = cacheList.size() - 1;   // 最新交易的 Index
            set.add(cacheList.get(lastIndex - i));
        }

        return set;
    }
}
