package com.fabric.display.service;

import com.fabric.display.bean.*;
import com.fabric.display.utils.TimeUtil;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.hyperledger.fabric.protos.common.Common;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.BlockInfo;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;

/**
 * @author Deng Zhiwen
 * @date 2020/7/15 15:55
 */
@Service
public class BlockInfoService {

    public BlockHeader getBlockHeader(BlockEvent blockEvent) {
        long blockNumber = blockEvent.getBlockNumber(); // 区块高度
        byte[] previousHashByte = blockEvent.getPreviousHash();
        String previousHash = base64Encode(previousHashByte);// 前一区块哈希

        Common.Block block = getNewestBlock(blockEvent);
        ByteString dataHashBS = block.getHeader().getDataHash();
        byte[] dataHashBytes = dataHashBS.toByteArray();
        String dataHash = base64Encode(dataHashBytes);  // 当前区块hash

        return new BlockHeader(blockNumber, dataHash, previousHash);
    }

    public BlockMataData getBlockMetaData(BlockEvent blockEvent) {
        Common.BlockMetadata metadata = blockEvent.getBlock().getMetadata();
        return new BlockMataData(base64Encode(metadata.toByteArray()));
    }

    public BlockData getBlockData(BlockEvent blockEvent) throws InvalidProtocolBufferException {
        // Common.Block block = getNewestBlock(blockEvent);
        int transactionCount = blockEvent.getTransactionCount();   // 当前区块中背书交易数（只有有效交易）
        int envelopeCount = blockEvent.getEnvelopeCount();      // 当前区块交易数（包含无效交易）
        String channelId = blockEvent.getChannelId();   // 通道名

        BlockData blockData = new BlockData();
        blockData.setChannelId(channelId);
        blockData.setEnvelopeCount(envelopeCount);
        blockData.setTransactionCount(transactionCount);

        Iterable<BlockInfo.EnvelopeInfo> envelopeInfos = blockEvent.getEnvelopeInfos();

        HashSet<DataTransaction> allTransactions = new HashSet<>(); // 当前区块所有交易
        HashSet<DataTransaction> validTransactions = new HashSet<>();   // 当前区块有效交易
        for (BlockInfo.EnvelopeInfo envelopeInfo : envelopeInfos) {
            boolean valid = envelopeInfo.isValid();    // 交易是否有效

            BlockInfo.EnvelopeInfo.IdentitiesInfo creator = envelopeInfo.getCreator();
            String creatorId = creator.getId();   // 创建者ID
            String creatorMSPId = creator.getMspid(); // 创建者MSP

            byte[] nonce = envelopeInfo.getNonce();
            String nonceStr = Base64.getEncoder().encodeToString(nonce);  // 交易随机数
            /*Date timestamp = envelopeInfo.getTimestamp();  // 时间戳
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String time = simpleDateFormat.format(timestamp);*/
            String time = TimeUtil.date2String(envelopeInfo.getTimestamp());    // 时间戳
            // long time = timestamp.getTime();

            String transactionID = envelopeInfo.getTransactionID();    // 交易ID
            String type = envelopeInfo.getType().toString();  // 类型
            byte validationCode = envelopeInfo.getValidationCode();    // 验证码

            DataTransaction sqlTransaction = new DataTransaction(valid, creatorMSPId, creatorId, nonceStr, time, type, transactionID, validationCode, envelopeInfo.getChannelId(), blockEvent.getBlockNumber());
            allTransactions.add(sqlTransaction);
            if (valid) {
                validTransactions.add(sqlTransaction);
            }
        }

        blockData.setAllTransactions(allTransactions);
        blockData.setValidTransactions(validTransactions);

        return blockData;
    }


    private String base64Encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private Common.Block getNewestBlock(BlockEvent blockEvent) {
        return blockEvent.getBlock();
    }

}
