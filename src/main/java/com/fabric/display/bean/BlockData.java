package com.fabric.display.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * @Author Deng Zhiwen
 * @Date 2020/7/3 13:35
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Component
public class BlockData {

    private String channelId;   // 通道名

    private Integer transactionCount;   // 当前区块背书交易数（只包含有效交易）

    private Integer envelopeCount;  // 当前区块所有交易数

    private Set<DataTransaction> allTransactions;     // 当前区块所有交易

    private Set<DataTransaction> validTransactions;    // 当前区块有效交易

}
