package com.fabric.display.controller;

import com.fabric.display.service.ListennerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Deng Zhiwen
 * @date 2020/7/30 9:46
 */
@Api(tags = "fabric-gateway-connection", description = "通过 gateway 连接网络")
@RestController
@RequestMapping("fabric/gateway")
@Slf4j
public class GatewayController {

    @Autowired
    private ListennerService listennerService;

    // 目前采用默认连接
    @GetMapping("/connection")
    @ApiOperation("目前采用默认 channel 连接，foochannel")
    public String connection() {
        String channelName = "foochannel";  // TODO 自己搭一套网络再测(表的字段还有问题，检查一下）
        return listennerService.connection(channelName) ? "Success!" : "Failed";
    }
}
