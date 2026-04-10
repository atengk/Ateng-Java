package io.github.atengk.basic.model;

/**
 * 用户实体类（示例）
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class User {

    private Long id;
    private String username;

    public User() {
    }

    public User(Long id, String username) {
        this.id = id;
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
