package com.example.msbaseprj.config.web.filter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class FilterConfig {
    
    @Bean
    public FilterRegistrationBean<LoggingFilter> loggingFilterRegistration() {
        var registrationBean = new FilterRegistrationBean<LoggingFilter>();

        registrationBean.setFilter(new LoggingFilter());
        registrationBean.addUrlPatterns("/api/*"); 
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE); 
        
        return registrationBean;
    }

}
