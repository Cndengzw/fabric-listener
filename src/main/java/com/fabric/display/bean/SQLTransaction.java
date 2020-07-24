package com.fabric.display.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @Author Deng Zhiwen
 * @Date 2020/7/7 11:09
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Component
@Table(name = "transactionscount")
public class SQLTransaction {

    @Id
    @KeySql(useGeneratedKeys = true)
    private Integer id;

    @Column(name = "timeType")
    private String timeType;

    private Integer year;
    private Integer month;
    private Integer day;
    private Integer hour;
    private Integer minute;

    private Integer count;
    @Column(name = "lastBlockNum")
    private Long lastBlockNumber;


}
