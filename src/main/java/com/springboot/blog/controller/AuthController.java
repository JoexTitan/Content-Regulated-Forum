package com.springboot.blog.controller;

import com.springboot.blog.exception.BlogAPIException;
import com.springboot.blog.payload.LoginDto;
import com.springboot.blog.payload.RegisterDTO;
import com.springboot.blog.service.AuthService;
import com.springboot.blog.utils.AppEnums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping(value = {"/login", "signin"})
    public ResponseEntity<Object> login(@RequestBody LoginDto loginDto, HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType != null && contentType.startsWith("application/json")) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "Unsupported media type", ErrorCode.UNSUPPORTED_MEDIA_TYPE);
        } return ResponseEntity.status(HttpStatus.OK)
                .header("state", "logged-in")
                .body(authService.login(loginDto));
    }

    @PostMapping(value = {"/register", "signup"})
    public ResponseEntity<Object> register(@RequestBody RegisterDTO registerDTO, HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType != null && contentType.startsWith("application/json")) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,
                    "Unsupported media type", ErrorCode.UNSUPPORTED_MEDIA_TYPE);
        } return ResponseEntity.status(HttpStatus.OK)
                .header("state", "registered")
                .body(authService.register(registerDTO));
    }
}
