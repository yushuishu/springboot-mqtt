package com.shuishu.demo.mqtt.common.utils.mqtt;


import com.alibaba.fastjson2.JSONObject;
import com.shuishu.demo.mqtt.common.domain.ApiResponse;
import com.shuishu.demo.mqtt.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author ：谁书-ss
 * @date ：2023-03-14 21:54
 * @IDE ：IntelliJ IDEA
 * @Motto ：ABC(Always Be Coding)
 * <p></p>
 * @description ：
 * <p></p>
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class MqttPushClient {
    private final MqttConfig mqttConfig;

    private static MqttClient client;

    private static MqttClient getClient() {
        return client;
    }

    private static void setClient(MqttClient client) {
        MqttPushClient.client = client;
    }


    /**
     * 客户端连接
     *
     * @param host      ip+端口
     * @param clientID  客户端Id
     * @param username  用户名
     * @param password  密码
     * @param timeout   超时时间
     * @param keepalive 保留数
     */
    public void connect(String host, String clientID, String username, String password, int timeout, int keepalive) {
        MqttClient client;
        try {
            client = new MqttClient(host, clientID, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setUserName(username);
            options.setPassword(password.toCharArray());
            options.setConnectionTimeout(timeout);
            options.setKeepAliveInterval(keepalive);
            MqttPushClient.setClient(client);
            try {
                client.setCallback(new PushCallback());
                client.connect(options);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 发布
     *
     * @param qos         连接方式
     * @param retained    是否保留
     * @param topic       主题
     * @param pushMessage 消息体
     */
    public ApiResponse<String> publish(int qos, boolean retained, String topic, String pushMessage) {
        MqttMessage message = new MqttMessage();
        message.setQos(qos);
        message.setRetained(retained);
        message.setPayload(pushMessage.getBytes());
        MqttTopic mTopic = MqttPushClient.getClient().getTopic(topic);
        if (null == mTopic) {
            log.error("topic not exist");
            throw new BusinessException("发布主题不能为空");
        }
        MqttDeliveryToken token;
        try {
            token = mTopic.publish(message);
            token.waitForCompletion();
            return ApiResponse.success();
        } catch (MqttException e) {
            e.printStackTrace();
            return ApiResponse.error();
        }
    }

    /**
     * 订阅某个主题
     *
     * @param topic 主题
     * @param qos   连接方式
     */
    public void subscribe(String topic, int qos) {
        log.info("开始订阅主题" + topic);
        try {
            MqttPushClient.getClient().subscribe(topic, qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Bean
    public MqttPushClient getMqttPushClient() {
        if (mqttConfig.isEnabled()) {
            String[] mqttTopic = mqttConfig.getDefaultTopic().split(",");
            if (mqttTopic.length == 0) {
                throw new BusinessException("yml配置defaultTopic 错误");
            }
            // 连接
            connect(mqttConfig.getHostUrl(),
                    mqttConfig.getClientId(),
                    mqttConfig.getUsername(),
                    mqttConfig.getPassword(),
                    mqttConfig.getTimeout(),
                    mqttConfig.getKeepalive()
            );
            for (int i = 0; i < mqttTopic.length; i++) {
                // 订阅主题
                subscribe(mqttTopic[i], 0);
            }
        }
        return this;
    }

    public class PushCallback implements MqttCallback {
        private static String topic_;
        private static String qos_;
        private static String msg_;

        @Override
        public void connectionLost(Throwable throwable) {
            // 连接丢失后，一般在这里面进行重连
            log.info("连接断开，可以做重连");
            if (client == null || !client.isConnected()) {
                getMqttPushClient();
            }
        }

        @Override
        public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
            // subscribe后得到的消息会执行到这里面
            log.info("接收消息主题 : " + topic);
            log.info("接收消息Qos : " + mqttMessage.getQos());
            log.info("接收消息内容 : " + new String(mqttMessage.getPayload()));
            topic_ = topic;
            qos_ = mqttMessage.getQos()+"";
            msg_ = new String(mqttMessage.getPayload());
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
            log.info("deliveryComplete---------" + iMqttDeliveryToken.isComplete());
        }

        /**
         * 别的 Controller层会调用这个方法，来 获取 接收到的硬件数据
         *
         * @return 数据
         */
        public String receive() {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("topic", topic_);
            jsonObject.put("qos", qos_);
            jsonObject.put("msg", msg_);
            return jsonObject.toString();
        }

    }

}
