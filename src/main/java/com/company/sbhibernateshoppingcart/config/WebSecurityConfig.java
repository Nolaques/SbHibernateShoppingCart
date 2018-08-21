package com.company.sbhibernateshoppingcart.config;

import com.company.sbhibernateshoppingcart.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter{

    @Autowired
    UserDetailsServiceImpl userDetailsService;

    @Bean
    public BCryptPasswordEncoder passwordEncoder(){
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        return bCryptPasswordEncoder;
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception{

        //setting service to find user in database
        //and setting passwordEncoder
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.csrf().disable();

        //requires login with role ROLE_EMPLOYEE or ROLE_MANAGER
        //if not it will redirect to /admin/login
        http.authorizeRequests().antMatchers("/admin/orderList", "/admin/order", "/admin/accountInfo")
                .access("hasAnyRole('ROLE_EMPLOYEE', 'ROLE_MANAGER')");

        //pages only for manager
        http.authorizeRequests().antMatchers("/admin/product").access("hasRole('ROLE_MANAGER')");

        //when user login, role XX. But access to the page requires YY role.
        // An AccessDeniedException will be thrown
        http.authorizeRequests().and().exceptionHandling().accessDeniedPage("/403");

        //configuration for login form
        http.authorizeRequests().and().formLogin()
                .loginProcessingUrl("/j_spring_security_check") // submit URL
                .loginPage("/admin/login")
                .defaultSuccessUrl("/admin/accountInfo")
                .failureUrl("/admin/login?error=true")
                .usernameParameter("userName")
                .passwordParameter("password")

                //configuration for the logout page
                //after logout go to home page
                .and().logout().logoutUrl("/admin/logout").logoutSuccessUrl("/");
    }
}
