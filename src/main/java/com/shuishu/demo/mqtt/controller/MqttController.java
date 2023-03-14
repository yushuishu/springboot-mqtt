package com.shuishu.demo.mqtt.controller;


import com.shuishu.demo.mqtt.common.domain.ApiResponse;
import com.shuishu.demo.mqtt.common.utils.mqtt.MqttPushClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ：谁书-ss
 * @date ：2023-03-14 22:17
 * @IDE ：IntelliJ IDEA
 * @Motto ：ABC(Always Be Coding)
 * <p></p>
 * @description ：mqtt test
 * <p></p>
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("mqtt")
public class MqttController {

    private final MqttPushClient mqttPushClient;

    @GetMapping("light/open")
    public ApiResponse<String> lightOpen() {
        return mqttPushClient.publish(0, false, "java", "light open");
    }

    @GetMapping("light/close")
    public ApiResponse<String> lightClose() {
        return mqttPushClient.publish(0, false, "java", "light close");
    }

}
