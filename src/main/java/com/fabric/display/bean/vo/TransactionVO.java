package com.fabric.display.bean.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author Deng Zhiwen
 * @date 2020/7/30 11:21
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
public class TransactionVO {
    private String transactionId;       // 交易ID
    private boolean valid;            // 交易是否有效
    private String creatorMSPId;        // 发起者 MSPID
    private String transactionTime;        // 交易时间
    private String transactionContent;   // 交易具体内容
}
