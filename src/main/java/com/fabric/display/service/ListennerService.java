package com.fabric.display.service;

import com.fabric.display.bean.*;
import com.fabric.display.bean.vo.BlockVO;
import com.fabric.display.bean.vo.TransactionVO;
import com.fabric.display.utils.TimeUtil;
import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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

    // 配置文件，证书，私钥
    private static final String DEFAULT_CONNECTION_YAML = System.getProperty("user.dir") + "\\suc_fabric\\connection-org1.yaml";
    private static final String DEFAULT_ADMIN_CERTIFICATE_PEM = System.getProperty("user.dir") + "\\suc_fabric\\cert.pem";
    private static final String DEFAULT_ADMIN_PRIVATE_KEY = System.getProperty("user.dir") + "\\suc_fabric\\priv_sk";

    private static final String DEFAULT_CHANNEL_NAME = "foochannel";
    private static final String DEFAULT_MSP_ID = "MSP-org1";


    // MAP & LIST
    private static Map<String, ArrayList<BlockEvent>> channelBlockMap = new ConcurrentHashMap<>();  // 存放 {channelName, List<BlockEvent>}
    
    List<DataTransaction> cacheList = Collections.synchronizedList(new ArrayList<>()); // 缓存（用于存放最新的交易）
    private static Map<String, BlockEvent> blockEventMap = new ConcurrentHashMap<>();

    private long lastBlockNumber;

    // SDK，给channel增加一个区块监听（目前没用）
    private void channelRegisterBlockListener(Channel channel) {
        log.info(channel.getName() + "  RegisterBlockListener");
        try {
            channel.registerBlockListener(blockEvent -> {
                String dataHash = Arrays.toString(blockEvent.getDataHash());
                log.info("RegisterBlockListener" + channel.getName() + " 监听到最新的了！dataHash: " + dataHash + ", BlockNumber: " + blockEvent.getBlockNumber());
                // 监听逻辑
                try {
                    lastBlockNumber = blockEvent.getBlockNumber();
                    blockEventMap.put(channel.getName(), blockEvent);
                    listenNewestBlock(blockEvent);
                } catch (InvalidProtocolBufferException | ParseException e) {
                    e.printStackTrace();
                }
            });
        } catch (InvalidArgumentException e) {
            e.printStackTrace();
        }
    }

    // 连接网络
    public boolean connection(String channelName) {
        Wallet wallet = Wallet.createInMemoryWallet();   // 内存 wallet

        Path networkConfigFile = Paths.get(DEFAULT_CONNECTION_YAML);
        Path certificatePem = Paths.get(DEFAULT_ADMIN_CERTIFICATE_PEM);
        Path privateKey = Paths.get(DEFAULT_ADMIN_PRIVATE_KEY);

        try {
            Wallet.Identity identity = Wallet.Identity.createIdentity(DEFAULT_MSP_ID, Files.newBufferedReader(certificatePem), Files.newBufferedReader(privateKey));
            wallet.put("userName", identity);
            Gateway.Builder builder = Gateway.createBuilder().
                    identity(wallet, "userName").
                    networkConfig(networkConfigFile)
                    .discovery(true);
            Gateway gateway = builder.connect();
            Network network = gateway.getNetwork(channelName);

            networkAddBlockListener(network);     // 给 network 增加一个区块监听
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // network 注册监听（network 启动时调用）
    private void networkAddBlockListener(Network network) {
        Channel channel = network.getChannel();
        String chanelName = channel.getName();
        log.info(chanelName + "  AddBlockListener");

        network.addBlockListener(blockEvent -> {
            String dataHash = Arrays.toString(blockEvent.getDataHash());
            log.info("AddBlockListener: " + chanelName + " 监听到最新的了！dataHash: " + dataHash + ", BlockNumber: " + blockEvent.getBlockNumber());
            // 监听逻辑
            try {
                lastBlockNumber = blockEvent.getBlockNumber();
                blockEventMap.put(chanelName, blockEvent);  // 存入 {通道名，区块事件} 到 blockEventMap

                listenNewestBlock(blockEvent);  // 有新的区块事件时，进行操作
            } catch (InvalidProtocolBufferException | ParseException e) {
                e.printStackTrace();
            }
        });
    }

    // 获取最新区块信息
    public String getLastBlockInfo(String channelName, Integer count) throws InvalidProtocolBufferException, ParseException {
        ArrayList<BlockEvent> blockEvents = channelBlockMap.get(channelName);

        if (blockEvents == null) {
            return "通道名输入错误或该通道启动监听功能后还未监听到任何区块";
        }

        ArrayList<BlockVO> blocks = new ArrayList<>();

        for (int i = 0; i < blockEvents.size(); i++) {
            BlockEvent blockEvent = blockEvents.get(blockEvents.size() - 1 - i);
            if (blocks.size() < count) {
                BlockHeader blockHeader = blockInfoService.getBlockHeader(blockEvent);
                BlockMataData blockMataData = blockInfoService.getBlockMetaData(blockEvent);
                BlockData blockData = blockInfoService.getBlockData(blockEvent);
                long createdTime = Long.MAX_VALUE;
                for (DataTransaction transaction : blockData.getAllTransactions()) {
                    String transactionTime = transaction.getTransactionTime();
                    long transactionTimeL = TimeUtil.string2Date(transactionTime).getTime();
                    createdTime = Math.min(createdTime, transactionTimeL);
                }
                BlockVO blockVO = new BlockVO();
                blockVO.setPreBlockHash(blockHeader.getPreviousHash());
                blockVO.setCurBlockHash(blockHeader.getBlockHash());
                blockVO.setBlockNumber(blockHeader.getBlockNumber());
                blockVO.setTransactionsNum(blockData.getAllTransactions().size());
                blockVO.setCreatedTime(createdTime);
                blocks.add(blockVO);  // TODO mataData
            } else break;
        }

        return gson.toJson(blocks);
    }

    // 监听到最新区块事件
    public void listenNewestBlock(BlockEvent blockEvent) throws InvalidProtocolBufferException, ParseException {
        log.info("listenNewestBlock: 监听到新的区块了，BlockNumber = " + blockEvent.getBlockNumber());

        // 得到所有区块交易
        BlockData blockData = blockInfoService.getBlockData(blockEvent);
        Set<DataTransaction> allTransactions = blockData.getAllTransactions();
        log.debug("listenNewestBlock: allTransactions.size() = " + allTransactions.size());

        for (DataTransaction transaction : allTransactions) {
            // 插入监听到的最新交易
            SQLTransaction sqlTransaction = dataTransaction2SQLTransaction(transaction);
            sqlTransactionService.insertOneTransaction(sqlTransaction);

            if (!cacheList.contains(transaction)) {
                cacheList.add(transaction);
            }
        }
        log.debug("listenNewestBlock: cacheList.size() = " + cacheList.size());
        while (cacheList.size() > 300) {    // 缓存最多保存 300条最新交易
            cacheList.remove(0);
        }

        lastBlockNumber = blockEvent.getBlockNumber();
    }


    // 定时任务，一天结束后统计当日数据并插入 TODO(根据通道不同统计不同的条数）
    public void insertOneDayTransactions() {
        SQLTransaction sqlTransaction = new SQLTransaction();

        LocalDateTime dateTime = LocalDateTime.now();

        sqlTransaction.setTimeType("day");
        sqlTransaction.setYear(dateTime.getYear());
        sqlTransaction.setMonth(dateTime.getMonthValue());
        sqlTransaction.setDay(dateTime.getDayOfMonth());

        String channelName = DEFAULT_CHANNEL_NAME;
        int count = sqlTransactionService.selectOneDayTransactions(channelName, dateTime.getYear(), dateTime.getMonthValue(), dateTime.getDayOfMonth());
        sqlTransaction.setCount(count);
        // sqlTransaction.setChannelName(channelName);
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
        if (month == 0) {
            // 该月是1月，上一个月应该为 12月
            month = 12;
        }

        String channelName = DEFAULT_CHANNEL_NAME;
        Map<Integer, Integer> daysTransactions = sqlTransactionService.selectOneMonthTransactions(channelName, dateTime.getYear(), month);
        int count = 0;

        Collection<Integer> daysTransactionsCount = daysTransactions.values();
        for (Integer daysTransaction : daysTransactionsCount) {
            count += daysTransaction;
        }
        sqlTransaction.setCount(count);
        sqlTransaction.setChannelName(channelName);
        sqlTransactionService.insertOneTransaction(sqlTransaction);
    }


    // 查询最新 N 个交易
    public Set<TransactionVO> selectNewestTransactions(Integer count) {
        log.debug("selectNewestTransactions : cacheList.size() = " + cacheList.size());

        Set<TransactionVO> set = new LinkedHashSet<>();
        for (int i = 0; i < cacheList.size(); i++) {
            if (set.size() < count) {
                DataTransaction dataTransaction = cacheList.get(cacheList.size() - 1 - i);
                TransactionVO transactionVO = dataTransaction2TransactionVO(dataTransaction);
                set.add(transactionVO);
            } else break;
        }

        return set; // 有序性由 LinkedHashSet 保证
    }

    // 把 DataTransaction 转换为 TransactionVO
    private TransactionVO dataTransaction2TransactionVO(DataTransaction dataTransaction) {
        TransactionVO transactionVO = new TransactionVO();

        transactionVO.setTransactionId(dataTransaction.getTransactionId());
        transactionVO.setValid(dataTransaction.isValid());
        transactionVO.setCreatorMSPId(dataTransaction.getCreatorMSPId());
        transactionVO.setTransactionTime(dataTransaction.getTransactionTime());
        transactionVO.setTransactionContent(null);

        return transactionVO;
    }

    // 把 DataTransaction 转换为 SQLTransaction
    private SQLTransaction dataTransaction2SQLTransaction(DataTransaction dataTransaction) throws ParseException {
        SQLTransaction sqlTransaction = new SQLTransaction();

        sqlTransaction.setTransactionId(dataTransaction.getTransactionId());
        sqlTransaction.setMspId(dataTransaction.getCreatorMSPId());
        sqlTransaction.setIsValid(dataTransaction.isValid());
        sqlTransaction.setChannelName(dataTransaction.getChannelName());

        String transactionTime = dataTransaction.getTransactionTime();
        sqlTransaction.setTimestamp(transactionTime);

        Date transactionDate = TimeUtil.string2Date(transactionTime);
        sqlTransaction.setYear(transactionDate.getYear());
        sqlTransaction.setMonth(transactionDate.getMonth() + 1);   // getMonth -> [0,11]
        sqlTransaction.setDay(transactionDate.getDate());
        sqlTransaction.setHour(transactionDate.getHours());
        sqlTransaction.setMinute(transactionDate.getMinutes());
        sqlTransaction.setSecond(transactionDate.getSeconds());

        sqlTransaction.setBlockNumber(dataTransaction.getBlockNumber());

        return sqlTransaction;
    }
}
