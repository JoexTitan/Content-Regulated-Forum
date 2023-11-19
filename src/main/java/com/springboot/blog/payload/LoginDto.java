package com.springboot.blog.payload;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginDto implements Serializable {
    private String username;
    private String password;
}
