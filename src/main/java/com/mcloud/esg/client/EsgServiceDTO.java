package com.mcloud.esg.client;

import com.mcloud.core.constant.PlatformEnum;

import lombok.Data;

/**
 * Esg聚合服务持久化对象.
 * 
 * @author liukai
 *
 */
@Data
public class EsgServiceDTO {

	/**
	 * UUID主键.
	 */
	private String securityGroupId;

	/**
	 * 平台ID. {@link PlatformEnum}
	 */
	private String platformId;

	/**
	 * 区域
	 */
	private String regionId;

	/**
	 * task对象,不持久化.
	 */
	private String taskId;

	/**
	 * 用户ID.
	 */
	private String username;

	/**
	 * 平台资源的唯一标识符
	 */
	private String securityGroupUuid = "";

	/**
	 * Vpc的主键.
	 */
	private String vpcId = "";

	/**
	 * 阿里云Vpc的UUID.
	 */
	private String vpcUuid = "";

	/**
	 * 说明.
	 */
	private String description = "";

	/**
	 * 说明.
	 */
	private String securityGroupName = "";

	/**
	 * 安全组类型.
	 */
	private String esgType;

}
