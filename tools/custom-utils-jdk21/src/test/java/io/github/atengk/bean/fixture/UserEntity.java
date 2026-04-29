package io.github.atengk.bean.fixture;

import java.time.LocalDate;

/**
 * 用户实体测试对象。
 *
 * @author Ateng
 * @since 2026-04-29
 */
@TestMarker
public class UserEntity {

    @TestMarker
    private Long id;

    private String username;

    private Integer age;

    private Boolean enabled;

    private LoginStatus status;

    private LocalDate birthday;

    private Address address;

    private final String immutableCode = "LOCKED";

    public UserEntity() {
    }

    public UserEntity(Long id, String username) {
        this.id = id;
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    @TestMarker
    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LoginStatus getStatus() {
        return status;
    }

    public void setStatus(LoginStatus status) {
        this.status = status;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getImmutableCode() {
        return immutableCode;
    }
}
