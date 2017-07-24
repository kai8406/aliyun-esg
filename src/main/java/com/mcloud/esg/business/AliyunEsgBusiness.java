package com.mcloud.esg.business;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.aliyuncs.IAcsClient;
import com.aliyuncs.ecs.model.v20140526.CreateSecurityGroupRequest;
import com.aliyuncs.ecs.model.v20140526.CreateSecurityGroupResponse;
import com.aliyuncs.ecs.model.v20140526.DeleteSecurityGroupRequest;
import com.aliyuncs.ecs.model.v20140526.DeleteSecurityGroupResponse;
import com.aliyuncs.exceptions.ClientException;
import com.mcloud.core.constant.ActiveEnum;
import com.mcloud.core.constant.AggTypeEnum;
import com.mcloud.core.constant.mq.MQConstant;
import com.mcloud.core.constant.result.ResultDTO;
import com.mcloud.core.constant.result.ResultEnum;
import com.mcloud.core.constant.task.TaskDTO;
import com.mcloud.core.constant.task.TaskStatusEnum;
import com.mcloud.core.mapper.BeanMapper;
import com.mcloud.esg.client.AccesskeyDTO;
import com.mcloud.esg.client.EsgServiceDTO;
import com.mcloud.esg.entity.AliyunEsgDTO;
import com.mcloud.esg.service.AliyunEsgService;

@Component
public class AliyunEsgBusiness extends AbstractAliyunCommon {

	@Autowired
	protected AliyunEsgService service;

	/**
	 * 根据阿里云的Id获得AliyunEsgDTO对象.
	 * 
	 * @param uuid
	 * @return
	 */
	private AliyunEsgDTO getAliyunEsgDTOByUUID(String uuid) {
		Map<String, Object> map = new HashMap<>();
		map.put("EQ_uuid", uuid);
		return service.find(map);
	}

	public void removeEsg(EsgServiceDTO esgServiceDTO) {

		// Step.1 创建Task对象.
		TaskDTO taskDTO = taskClient.getTask(esgServiceDTO.getTaskId());

		// Step.2 获得AliyunRouterDTO对象,并更新状态.
		AccesskeyDTO accesskeyDTO = accountClient
				.getAccesskey(esgServiceDTO.getUsername(), esgServiceDTO.getPlatformId()).getData();

		// Step.3 获得AliyunVSwtichDTO.
		AliyunEsgDTO aliyunEsgDTO = getAliyunEsgDTOByUUID(esgServiceDTO.getSecurityGroupUuid());

		// Step.4 调用阿里云SDK执行操作.
		DeleteSecurityGroupRequest request = new DeleteSecurityGroupRequest();
		request.setSecurityGroupId(aliyunEsgDTO.getUuid());

		IAcsClient client = getServiceInstance(esgServiceDTO.getRegionId(), accesskeyDTO);

		DeleteSecurityGroupResponse response = null;

		try {
			response = client.getAcsResponse(request);

			taskDTO.setRequestId(response.getRequestId());

		} catch (ClientException e) {

			taskDTO.setStatus(TaskStatusEnum.执行失败.name());
			taskDTO.setResponseCode(e.getErrCode());
			taskDTO.setResponseData(e.getErrMsg());
			taskDTO = taskClient.updateTask(taskDTO.getId(), taskDTO);

			ResultDTO resultDTO = new ResultDTO(esgServiceDTO.getSecurityGroupId(), AggTypeEnum.esg.name(),
					ResultEnum.ERROR.name(), taskDTO.getId(), esgServiceDTO.getUsername(),
					esgServiceDTO.getSecurityGroupUuid());

			rabbitTemplate.convertAndSend(MQConstant.MQ_EXCHANGE_NAME, MQConstant.ROUTINGKEY_RESULT_REMOVE,
					binder.toJson(resultDTO));
			return;

		}

		taskDTO.setStatus(TaskStatusEnum.执行成功.name());
		taskDTO = taskClient.updateTask(taskDTO.getId(), taskDTO);

		aliyunEsgDTO.setActive(ActiveEnum.N.name());
		aliyunEsgDTO = service.saveAndFlush(aliyunEsgDTO);

		ResultDTO resultDTO = new ResultDTO(esgServiceDTO.getSecurityGroupId(), AggTypeEnum.esg.name(),
				ResultEnum.SUCCESS.name(), taskDTO.getId(), esgServiceDTO.getUsername(),
				esgServiceDTO.getSecurityGroupUuid());

		rabbitTemplate.convertAndSend(MQConstant.MQ_EXCHANGE_NAME, MQConstant.ROUTINGKEY_RESULT_REMOVE,
				binder.toJson(resultDTO));
	}

	public void saveEsg(EsgServiceDTO esgServiceDTO) {

		// Step.1 创建Task对象.
		TaskDTO taskDTO = taskClient.getTask(esgServiceDTO.getTaskId());

		// Step.2 获得AliyunRouterDTO对象,并更新状态.
		AccesskeyDTO accesskeyDTO = accountClient
				.getAccesskey(esgServiceDTO.getUsername(), esgServiceDTO.getPlatformId()).getData();

		// Step.3 持久化AliyunVSwtichDTO.
		AliyunEsgDTO aliyunEsgDTO = BeanMapper.map(esgServiceDTO, AliyunEsgDTO.class);
		aliyunEsgDTO.setSecurityGroupId(esgServiceDTO.getSecurityGroupId());
		aliyunEsgDTO.setCreateTime(new Date());

		aliyunEsgDTO = service.saveAndFlush(aliyunEsgDTO);

		// Step.4 调用阿里云SDK执行操作
		CreateSecurityGroupRequest request = new CreateSecurityGroupRequest();
		request.setSecurityGroupName(aliyunEsgDTO.getSecurityGroupName());
		request.setDescription(aliyunEsgDTO.getDescription());
		request.setRegionId(aliyunEsgDTO.getRegionId());
		request.setVpcId(aliyunEsgDTO.getVpcUuid());

		IAcsClient client = getServiceInstance(aliyunEsgDTO.getRegionId(), accesskeyDTO);

		CreateSecurityGroupResponse response = null;

		try {
			response = client.getAcsResponse(request);

			taskDTO.setRequestId(response.getRequestId());

		} catch (ClientException e) {

			// 修改Task对象执行状态.
			taskDTO.setStatus(TaskStatusEnum.执行失败.name());
			taskDTO.setResponseCode(e.getErrCode());
			taskDTO.setResponseData(e.getErrMsg());
			taskDTO = taskClient.updateTask(taskDTO.getId(), taskDTO);

			ResultDTO resultDTO = new ResultDTO(esgServiceDTO.getSecurityGroupId(), AggTypeEnum.esg.name(),
					ResultEnum.ERROR.name(), taskDTO.getId(), esgServiceDTO.getUsername(), "");

			// 将执行的结果进行广播.
			rabbitTemplate.convertAndSend(MQConstant.MQ_EXCHANGE_NAME, MQConstant.ROUTINGKEY_RESULT_SAVE,
					binder.toJson(resultDTO));
			return;
		}

		aliyunEsgDTO.setUuid(response.getSecurityGroupId());
		aliyunEsgDTO = service.saveAndFlush(aliyunEsgDTO);

		taskDTO.setStatus(TaskStatusEnum.执行成功.name());
		taskDTO = taskClient.updateTask(taskDTO.getId(), taskDTO);

		ResultDTO resultDTO = new ResultDTO(esgServiceDTO.getSecurityGroupId(), AggTypeEnum.esg.name(),
				ResultEnum.SUCCESS.name(), taskDTO.getId(), esgServiceDTO.getUsername(), aliyunEsgDTO.getUuid());

		// Step.7 将执行的结果进行广播.
		rabbitTemplate.convertAndSend(MQConstant.MQ_EXCHANGE_NAME, MQConstant.ROUTINGKEY_RESULT_SAVE,
				binder.toJson(resultDTO));

	}

}
