package com.mykaarma.kcommunications.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.mykaarma.kcommunications.model.jpa.PredictionFeature;

@Transactional
public interface PredictionFeatureRepository extends JpaRepository<PredictionFeature, Long>{

	@Query(value="select pf.id from PredictionFeature pf where pf.predictionFeature= :predictionFeature",nativeQuery=true)
	public Long findIdByPredictionFeature(@Param("predictionFeature") String predictionFeature);
	
	public PredictionFeature findByPredictionFeature(String predictionFeature);

}