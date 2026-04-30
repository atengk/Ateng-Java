package io.github.atengk.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.time.LocalDateTime;

final class TestModels {

    private TestModels() {
    }

    @JacksonXmlRootElement(localName = "user")
    public static class User {
        private String name;
        private Integer age;
        private Boolean active;

        public User() {
        }

        public User(String name, Integer age, Boolean active) {
            this.name = name;
            this.age = age;
            this.active = active;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public Boolean getActive() {
            return active;
        }

        public void setActive(Boolean active) {
            this.active = active;
        }
    }

    @JacksonXmlRootElement(localName = "person")
    public static class Person {
        private String firstName;
        private LocalDateTime createdAt;

        public Person() {
        }

        public Person(String firstName, LocalDateTime createdAt) {
            this.firstName = firstName;
            this.createdAt = createdAt;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }

    @JacksonXmlRootElement(localName = "book")
    public static class Book {
        @JacksonXmlProperty(isAttribute = true)
        private String id;
        private String title;

        public Book() {
        }

        public Book(String id, String title) {
            this.id = id;
            this.title = title;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    public enum Status {
        ENABLED,
        DISABLED
    }
}
