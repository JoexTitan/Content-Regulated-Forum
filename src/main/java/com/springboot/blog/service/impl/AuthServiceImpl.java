package com.springboot.blog.service.impl;

import com.springboot.blog.jwt.MyUserDetailsService;
import com.springboot.blog.entity.RoleEntity;
import com.springboot.blog.entity.UserEntity;
import com.springboot.blog.jwt.JwtTokenProvider;
import com.springboot.blog.payload.LoginDto;
import com.springboot.blog.payload.RegisterDTO;
import com.springboot.blog.repository.RoleRepository;
import com.springboot.blog.repository.UserRepository;
import com.springboot.blog.service.AuthService;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {
    final UserRepository userRepository;
    final RoleRepository roleRepository;
    final PasswordEncoder passwordEncoder;
    final JwtTokenProvider jwtTokenProvider;
    final MyUserDetailsService myUserDetailsService;
    final AuthenticationManager authenticationManager;

    public String login(LoginDto loginDto) {
        // Created authentication object to add to the SecurityContextHolder
        Authentication authenticated = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authenticated);
        return "Authenticated Successfully: Bearer " + jwtTokenProvider.generateToken(authenticated);
    }

    public String register(RegisterDTO registerDTO) {
        // RegistrationValidationFilter will execute prior to this method
        UserEntity newUser = new UserEntity();
        // extracting data from dto & saving newUser
        newUser.setName(registerDTO.getName());
        newUser.setEmail(registerDTO.getEmail());
        newUser.setUsername(registerDTO.getUsername());
        newUser.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        newUser.setFavBlogGenres(new ArrayList<>());

        Set<RoleEntity> role = new HashSet<>();
        role.add(roleRepository.findByName("ROLE_USER").get());
        newUser.setRoles(role);

        userRepository.save(newUser);

        return "Registered: " + newUser.getName() + " Successfully!";
    }
}
