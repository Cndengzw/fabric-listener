package com.fabric.display;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

/**
 * @Author Deng Zhiwen
 * @Date 2020/6/30 14:57
 */
@SpringBootTest
@RunWith(SpringRunner.class)
@Slf4j
public class MyTest {

    @Test
    public void test() throws ParseException {
        String transactionTime = "2020-09-07 14:22:15";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = simpleDateFormat.parse(transactionTime);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);



        LocalDateTime localDateTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        int year1 = localDateTime.getYear();
        int monthValue = localDateTime.getMonthValue();
        int dayOfMonth = localDateTime.getDayOfMonth();
        int hour1 = localDateTime.getHour();
        int minute1 = localDateTime.getMinute();
        int second1 = localDateTime.getSecond();
        int dayOfYear = localDateTime.getDayOfYear();
        DayOfWeek dayOfWeek = localDateTime.getDayOfWeek();

        System.out.println("year1:" + year);

    }

}
