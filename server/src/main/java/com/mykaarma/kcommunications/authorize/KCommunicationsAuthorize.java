package com.mykaarma.kcommunications.authorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.mykaarma.kcommunications_model.enums.ApiScope;
import com.mykaarma.kcommunications_model.enums.ApiScopeLevel;


@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface KCommunicationsAuthorize {

    ApiScope apiScope();
    ApiScopeLevel apiScopeLevel();
}
