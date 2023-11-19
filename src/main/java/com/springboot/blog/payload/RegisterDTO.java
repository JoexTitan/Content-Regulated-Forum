package com.springboot.blog.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDTO implements Serializable {
    private String name;
    private String email;
    private String username;
    private String password;
}
