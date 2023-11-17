package com.springboot.blog.config;

import com.springboot.blog.filters.CommentValidationFilter;
import com.springboot.blog.filters.RegistrationValidationFilter;
import com.springboot.blog.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    private final UserRepository userRepository;
    @Autowired
    public FilterConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    @Bean
    public FilterRegistrationBean<RegistrationValidationFilter> registrationFilter() {
        FilterRegistrationBean<RegistrationValidationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RegistrationValidationFilter(userRepository));
        registrationBean.setOrder(0);
        registrationBean.addUrlPatterns("/api/auth/register", "/api/auth/signup");
        return registrationBean;
    }
    @Bean
    public FilterRegistrationBean<CommentValidationFilter> commentFilter() {
        FilterRegistrationBean<CommentValidationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new CommentValidationFilter(userRepository));
        registrationBean.setOrder(1);
        registrationBean.addUrlPatterns("/api/auth/posts/**");
        return registrationBean;
    }
}
