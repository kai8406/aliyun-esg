package com.mcloud.esg.mq;

import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import com.mcloud.core.constant.mq.MQConstant;

public interface AliyunEsgMQService {

	@RabbitListener(bindings = @QueueBinding(value = @Queue(value = "cmop.agg.aliyun.esg", durable = "false", autoDelete = "true"), key = "cmop.agg.esg.*", exchange = @Exchange(value = MQConstant.MQ_EXCHANGE_NAME, type = ExchangeTypes.TOPIC)))
	public void aliyunEsgAgg(Message message);

}
