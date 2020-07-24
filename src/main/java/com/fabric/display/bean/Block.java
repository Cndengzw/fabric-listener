package com.fabric.display.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @Author Deng Zhiwen
 * @Date 2020/7/2 16:16
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Component
public class Block {

    private BlockHeader blockHeader;

    private BlockData blockData;

    private BlockMataData blockMataData;

}
