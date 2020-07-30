package com.fabric.display.bean.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author Deng Zhiwen
 * @date 2020/7/30 11:07
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
public class BlockVO {
    private String curBlockHash;        // 当前区块哈希
    private String preBlockHash;        // 前 1 区块哈希
    private Long blockNumber;           // 当前区块编号（区块高度）
    private Integer transactionsNum;    // 此区块交易数目
    private Long createdTime;         // 区块产生时间
}
