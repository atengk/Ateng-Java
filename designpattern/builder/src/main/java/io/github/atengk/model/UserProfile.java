package io.github.atengk.model;

/**
 * 用户档案实体类
 */
public class UserProfile {

    private final String username;
    private final String email;
    private final String address;
    private final String phone;
    private final Integer age;
    private final boolean active;
    private final boolean admin;

    /**
     * 构造函数设为私有，禁止外部直接创建
     */
    private UserProfile(UserProfileBuilder builder) {
        this.username = builder.username;
        this.email = builder.email;
        this.address = builder.address;
        this.phone = builder.phone;
        this.age = builder.age;
        this.active = builder.active;
        this.admin = builder.admin;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }

    public Integer getAge() {
        return age;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isAdmin() {
        return admin;
    }

    @Override
    public String toString() {
        return "UserProfile{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", address='" + address + '\'' +
                ", phone='" + phone + '\'' +
                ", age=" + age +
                ", active=" + active +
                ", admin=" + admin +
                '}';
    }

    public static UserProfile.UserProfileBuilder builder() {
        return new UserProfile.UserProfileBuilder();
    }

    /**
     * 内部构建者类
     */
    public static class UserProfileBuilder {
        private String username;
        private String email;
        private String address;
        private String phone;
        private Integer age;
        private boolean active = true;
        private boolean admin = false;

        public UserProfileBuilder username(String username) {
            this.username = username;
            return this;
        }

        public UserProfileBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserProfileBuilder address(String address) {
            this.address = address;
            return this;
        }

        public UserProfileBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public UserProfileBuilder age(Integer age) {
            this.age = age;
            return this;
        }

        public UserProfileBuilder active(boolean active) {
            this.active = active;
            return this;
        }

        public UserProfileBuilder admin(boolean admin) {
            this.admin = admin;
            return this;
        }

        /**
         * 构建最终对象
         */
        public UserProfile build() {
            if (username == null || email == null) {
                throw new IllegalArgumentException("用户名和邮箱不能为空");
            }
            return new UserProfile(this);
        }
    }
}
