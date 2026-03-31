package io.github.atengk.interceptor.dto;


import io.github.atengk.interceptor.annotation.FieldValid;

/**
 * 用户DTO
 */
public class UserDTO {

    @FieldValid(required = true, message = "用户名不能为空")
    private String username;

    @FieldValid(required = true, minLength = 6, message = "密码至少6位")
    private String password;

    // getter/setter 省略
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
