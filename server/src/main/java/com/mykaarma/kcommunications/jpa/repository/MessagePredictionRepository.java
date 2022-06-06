package com.mykaarma.kcommunications.jpa.repository;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mykaarma.kcommunications.model.jpa.MessagePrediction;

@Transactional
public interface MessagePredictionRepository extends JpaRepository<MessagePrediction, Long>{
	
	@SuppressWarnings("unchecked")
	public MessagePrediction saveAndFlush(MessagePrediction messagePrediction);
	
	@Modifying(clearAutomatically = true)
	@Query(value = "Insert Ignore Into MessagePrediction(`MessageID`,`PredictionFeatureID`,`Prediction`, `MetaData`)" 
			 + 	" values(:messageID, :predictionFeatureID, :prediction, :metadata) ", nativeQuery = true)
	public void insertMessagePrediction(@Param("messageID")Long messageID, @Param("predictionFeatureID")Long predictionFeatureID, @Param("prediction")String prediction
			, @Param("metadata")String metadata);

    @Query(value = "select ID from MessagePrediction"
        + "  where messageID = :messageID and  predictionFeatureID = :predictionFeatureID", nativeQuery = true)
    public Long getMessagePredictionIDForMessageIDAndPredictionFeatureID(@Param("messageID") Long messageID, @Param("predictionFeatureID") Long predictionFeatureID);
    
    @Query(value = "select * from MessagePrediction mp "
    		+ "left join PredictionFeature pf on mp.PredictionFeatureID=pf.ID "
    		+ "left join MessagePredictionFeedback mpf on mpf.MessagePredictionID= mp.ID and mpf.UserUuid= :userUuid "
    		+ " where mp.MessageID in ( :messageIds ) ", nativeQuery = true)
    public List<Object[]> findMessagePredictionForMessageIdListAndUserUuid(@Param("messageIds")List<Long> messageIds,@Param("userUuid")String userUuid);
    
    public List<MessagePrediction> findByMessageIDIn(List<Long> messageIds);
}
