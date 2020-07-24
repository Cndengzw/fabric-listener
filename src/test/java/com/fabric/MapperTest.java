package com.fabric;

import com.fabric.display.bean.SQLTransaction;
import com.fabric.display.service.SQLTransactionService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

/**
 * @Author Deng Zhiwen
 * @Date 2020/7/7 11:19
 */
@SpringBootTest
@RunWith(SpringRunner.class)
@Slf4j
public class MapperTest {


    @Autowired
    private SQLTransactionService sqlTransactionService;



    @Test
    public void insertAllTransactions() {

    }
}
