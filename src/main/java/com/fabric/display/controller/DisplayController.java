package com.fabric.display.controller;

import com.fabric.display.bean.*;
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
    @Autowired
    private BlockInfoService blockInfoService;
    @Autowired
    private Gson gson;

    private static final String defaultChannelName = "foochannel";
    private static final String mspID = "MSP-org1";
    private static Channel channel = null;
    private static Network network = null;
    private static Map<String, BlockEvent> map = new ConcurrentHashMap<>();

    // 连接网络
    @PostConstruct
    public void init() throws Exception {
        Wallet wallet = Wallet.createInMemoryWallet();   // 内存 wallet
//         String connectionYaml = "src/main/resources/suc_fabric/connection-org1.yaml";
        String connectionYaml = System.getProperty("user.dir") + "\\suc_fabric\\connection-org1.yaml";
        Path networkConfigFile = Paths.get(connectionYaml);

//         String adminCertificatePem = "src/main/resources/suc_fabric/cert.pem";
        String adminCertificatePem = System.getProperty("user.dir") + "\\suc_fabric\\cert.pem";
        Path certificatePem = Paths.get(adminCertificatePem);

//         String adminPricateKey = "src/main/resources/suc_fabric/priv_sk";
         String adminPricateKey = System.getProperty("user.dir") + "\\suc_fabric\\priv_sk";
        Path privateKey = Paths.get(adminPricateKey);

        try {
            Wallet.Identity identity = Wallet.Identity.createIdentity(mspID, Files.newBufferedReader(certificatePem), Files.newBufferedReader(privateKey));
            wallet.put("userName", identity);
            Gateway.Builder builder = Gateway.createBuilder().
                    identity(wallet, "userName").
                    networkConfig(networkConfigFile)
                    .discovery(true);
            Gateway defaultGateway = builder.connect();
            network = defaultGateway.getNetwork(defaultChannelName);
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*channel = network.getChannel();
         channel.registerBlockListener(blockEvent -> {
            System.out.println("blockEvent 进来了..;.");
            // 监听逻辑
            try {
                System.out.println("监听到了！  " + blockEvent.getBlockNumber());
                listenerService.listenNewestBlock(blockEvent);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
            map.put("blockEvent", blockEvent);
            }
        );*/

        network.addBlockListener(blockEvent -> {
            String dataHash = Arrays.toString(blockEvent.getDataHash());
            log.info("addBlockListener" + network.getChannel().getName() + " 监听到最新的了！dataHash: " + dataHash + ", BlockNumber: " + blockEvent.getBlockNumber());
            // 监听逻辑
            try {
                listenerService.listenNewestBlock(blockEvent);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
            map.put(network.getChannel().getName(), blockEvent);
        });
    }

    @ApiOperation("获得最新区块信息")
    @GetMapping("getNewestBlockInfo")
    public String getNewestBlockInfo(@RequestParam("channelName") String channelName) throws InvalidProtocolBufferException {
        BlockEvent blockEvent = map.get(channelName); // 最新区块事件
        if (blockEvent == null) {
            return "通道名输入错误或该通道启动监听功能后还未监听到任何区块";
        }

        BlockHeader blockHeader = blockInfoService.getBlockHeader(blockEvent);
        BlockMataData blockMataData = blockInfoService.getBlockMetaData(blockEvent);
        BlockData blockData = blockInfoService.getBlockData(blockEvent);

        return gson.toJson(new Block(blockHeader, blockData, blockMataData));
    }

    @ApiOperation("获得最新交易动态")
    @GetMapping("getNewestTransactions")
    public Set<DataTransaction> getNewestTransactions(@RequestParam("number") Integer number) {
        return listenerService.selectNewestTransactions(number);
    }

    @GetMapping("selectOneYearTransactions")
    @ApiOperation("查询年交易趋势")
    public Map<Integer, Integer> selectOneYearTransactions(@RequestParam("year") Integer year) {
        return sqlTransactionService.selectOneYearTransactions(year);
    }

    @GetMapping("selectOneMonthTransactions")
    @ApiOperation("查询月交易趋势")
    public Map<Integer, Integer> selectOneMonthTransactions(@RequestParam("year") Integer year, @RequestParam("month") Integer month) {
        return sqlTransactionService.selectOneMonthTransactions(year, month);
    }


}
