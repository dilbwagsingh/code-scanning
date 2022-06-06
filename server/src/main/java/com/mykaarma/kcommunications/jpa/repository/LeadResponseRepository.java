package com.mykaarma.kcommunications.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.mykaarma.kcommunications.model.jpa.LeadResponse;

@Transactional
public interface LeadResponseRepository  extends JpaRepository<LeadResponse, Long>  {

	@SuppressWarnings("unchecked")
	public LeadResponse saveAndFlush(LeadResponse leadResponse);
}