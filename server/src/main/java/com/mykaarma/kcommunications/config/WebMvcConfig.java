package com.mykaarma.kcommunications.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.mykaarma.kcommunications.authorize.AuthorizeHandlerInterceptor;
import com.mykaarma.kcommunications_model.common.RestURIConstants;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {


    private final AuthorizeHandlerInterceptor authorizeHandlerInterceptor;

    @Autowired
    public WebMvcConfig(AuthorizeHandlerInterceptor authorizeHandlerInterceptor) {
        this.authorizeHandlerInterceptor = authorizeHandlerInterceptor;
    }

    /**
     * Add Interceptors
     *
     * @param registry InterceptorRegistry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authorizeHandlerInterceptor)
        	.addPathPatterns("/department/**", "/dealer/**", "/file/**","/translate/**", "/" + RestURIConstants.EXTERNAL + "/" + RestURIConstants.MESSAGE);
    }

    /**
     * Add Views and custom Redirection
     *
     * @param registry ViewControllerRegistry
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
    	registry.addViewController("/").setViewName("redirect:/communications");
        registry.addViewController("/").setViewName("redirect:/swagger-ui.html");
    }
}

