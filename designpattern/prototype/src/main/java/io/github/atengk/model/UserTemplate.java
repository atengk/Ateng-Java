package io.github.atengk.model;


import java.io.Serializable;

/**
 * 用户模板类，支持克隆操作
 * 实现 Cloneable 接口以支持浅拷贝
 */
public class UserTemplate implements Cloneable, Serializable {

    private String username;
    private String role;
    private String email;

    public UserTemplate(String username, String role, String email) {
        this.username = username;
        this.role = role;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public String getEmail() {
        return email;
    }

    /**
     * 克隆当前对象
     */
    @Override
    public UserTemplate clone() {
        try {
            return (UserTemplate) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("克隆失败", e);
        }
    }

    @Override
    public String toString() {
        return "UserTemplate{" +
                "username='" + username + '\'' +
                ", role='" + role + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
