package com.mcloud.esg.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mcloud.core.persistence.BaseEntityCrudServiceImpl;
import com.mcloud.esg.entity.AliyunEsgDTO;
import com.mcloud.esg.repository.AliyunEsgRepository;

@Service
@Transactional
public class AliyunEsgService extends BaseEntityCrudServiceImpl<AliyunEsgDTO, AliyunEsgRepository> {

	@Autowired
	private AliyunEsgRepository repository;

	@Override
	protected AliyunEsgRepository getRepository() {
		return repository;
	}

}
