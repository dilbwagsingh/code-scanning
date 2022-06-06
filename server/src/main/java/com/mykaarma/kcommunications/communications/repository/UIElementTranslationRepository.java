package com.mykaarma.kcommunications.communications.repository;

import com.mykaarma.kcommunications.cache.CacheConfig;
import com.mykaarma.kcommunications.communications.model.jpa.UIElementTranslation;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface UIElementTranslationRepository extends JpaRepository<UIElementTranslation, Long> {
    
    @Cacheable(value = CacheConfig.COMMUNICATIONS_TEXT_TRANSLATION_CACHE, keyGenerator = "customKeyGenerator", unless="#result == null")
    @Query(value = "SELECT ElementValue from UIElementTranslation WHERE ElementKey = :elementKey AND Locale = :locale", nativeQuery = true)
    public String getTranslatedText(@Param("elementKey") String elementKey, @Param("locale") String locale);
}
