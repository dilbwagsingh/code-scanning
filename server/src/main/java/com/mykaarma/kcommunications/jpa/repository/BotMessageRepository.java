package com.mykaarma.kcommunications.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mykaarma.kcommunications.model.jpa.BotMessage;

public interface BotMessageRepository extends JpaRepository<BotMessage, Long> {

    @Query(value = "SELECT bm.* from BotMessage bm WHERE"
        + " bm.messageType = 'S' AND bm.protocol = 'X'"
        + " AND bm.toNumber = :fromNumber and bm.messagePurpose = 'OOO:TO'"
        + " ORDER BY bm.sentOn DESC LIMIT 1" , nativeQuery = true)
    BotMessage getLatestTurnOffBotMessageForNumber(@Param("fromNumber") String fromNumber);

    BotMessage findByUuid(String uuid);
}
