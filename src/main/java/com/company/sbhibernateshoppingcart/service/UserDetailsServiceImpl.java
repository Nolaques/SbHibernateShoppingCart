package com.company.sbhibernateshoppingcart.service;


import com.company.sbhibernateshoppingcart.dao.AccountDAO;
import com.company.sbhibernateshoppingcart.entity.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService{

    @Autowired
    private AccountDAO accountDAO;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = accountDAO.findAccount(username);
        System.out.println("Account= " + account);

        if (account == null){
            throw new UsernameNotFoundException("User " + username + "was not found in the database");

        }

        //employee, manager
        String role = account.getUserRole();

        List<GrantedAuthority> grantList = new ArrayList<>();

        //role_employee, role_manager
        GrantedAuthority authority = new SimpleGrantedAuthority(role);

        grantList.add(authority);

        boolean enabled = account.isActive();
        boolean accountNonExpired = true;
        boolean credentialNonExpired = true;
        boolean accountNonLocked = true;

        UserDetails userDetails = new User(account.getUserName(), account.getEncryptedPassword(),
                                        enabled, accountNonExpired,credentialNonExpired, accountNonLocked, grantList);

        return userDetails;
    }
}
