package com.springboot.blog.jwt;

import com.springboot.blog.entity.UserEntity;
import com.springboot.blog.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class MyUserDetailsService implements UserDetailsService {

    final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // find the user by username in order to return User(username, password, GrantedAuthorities roles)
        UserEntity foundUser = userRepository.findByUsername(username).orElseThrow(
                () -> new UsernameNotFoundException("User with username: " + username + "was not found!"));

        // creating a set of Granted authorities in order to pass it to the User Object
        Set<GrantedAuthority> userRoles = foundUser.getRoles()
                .stream().map(role -> new SimpleGrantedAuthority(role.getName())).collect(Collectors.toSet());

        return new User(foundUser.getUsername(), foundUser.getPassword(), userRoles);

    }
}
