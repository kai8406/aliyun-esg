package com.mcloud.esg.mq;

import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mcloud.core.constant.PlatformEnum;
import com.mcloud.core.constant.mq.MQConstant;
import com.mcloud.core.mapper.JsonMapper;
import com.mcloud.core.util.EncodeUtils;
import com.mcloud.esg.business.AliyunEsgBusiness;
import com.mcloud.esg.client.EsgServiceDTO;

@Component
public class AliyunEsgMQServiceImpl implements AliyunEsgMQService {

	private static JsonMapper binder = JsonMapper.nonEmptyMapper();

	@Autowired
	private AliyunEsgBusiness business;

	@Override
	public void aliyunEsgAgg(Message message) {

		String receivedRoutingKey = message.getMessageProperties().getReceivedRoutingKey();

		String receiveString = EncodeUtils.EncodeMessage(message.getBody());

		EsgServiceDTO esgServiceDTO = binder.fromJson(receiveString, EsgServiceDTO.class);

		if (!PlatformEnum.aliyun.name().equalsIgnoreCase(esgServiceDTO.getPlatformId())) {
			return;
		}

		if (MQConstant.ROUTINGKEY_AGG_ESG_SAVE.equalsIgnoreCase(receivedRoutingKey)) {

			business.saveEsg(esgServiceDTO);

		} else if (MQConstant.ROUTINGKEY_AGG_ESG_UPDATE.equalsIgnoreCase(receivedRoutingKey)) {

		} else if (MQConstant.ROUTINGKEY_AGG_ESG_REMOVE.equalsIgnoreCase(receivedRoutingKey)) {

			business.removeEsg(esgServiceDTO);
		}
	}

}
