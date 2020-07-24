package com.fabric.display.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @Author Deng Zhiwen
 * @Date 2020/7/3 13:44
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Component
public class BlockHeader {

    private long blockNumber;

    private String blockHash;

    private String previousHash;
}
