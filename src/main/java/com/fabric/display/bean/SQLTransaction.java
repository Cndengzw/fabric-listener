package com.fabric.display.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

/**
 * @Author Deng Zhiwen
 * @Date 2020/7/7 11:09
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Component
@Table(name = "transactions")
public class SQLTransaction {

    @Id
    @KeySql(useGeneratedKeys = true)
    private Integer id;

    private String transactionId;
    private String mspId;
    private Boolean isValid;
    private String channelName;

    private String timeType;
    private String timestamp;
    private Integer year;
    private Integer month;
    private Integer day;
    private Integer hour;
    private Integer minute;
    private Integer second;

    private Long blockNumber;
    private Integer count;

}
