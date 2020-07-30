package com.fabric.display.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @Author Deng Zhiwen
 * @Date 2020/7/6 11:10
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Component
public class DataTransaction {
    private boolean valid;  // 交易是否有效
    private String creatorMSPId;    // 创建者MSPId
    private String creatorId;   // 创建者ID（证书）
    private String nonce;    // 交易随机数
    private String transactionTime; // 交易时间
    private String type;    // 类型
    private String transactionId;   // 交易ID
    private byte validationCode;    // 验证码
    private String channelName;     // 该交易所属通道
    private Long blockNumber;   // 该交易所在区块
}
