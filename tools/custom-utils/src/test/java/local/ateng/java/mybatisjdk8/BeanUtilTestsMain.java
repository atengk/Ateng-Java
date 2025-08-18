package local.ateng.java.mybatisjdk8;

import local.ateng.java.customutils.utils.BeanUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class BeanUtilTestsMain {
    public static void main(String[] args) {
        User user1 = new User();
        user1.setName("Tom");
        user1.setAddress(new Address("Beijing"));
        user1.setTags(new ArrayList<>());
        user1.getTags().add("developer");

        //User user2 = new User();

        // 使用你写的浅拷贝方法
        User user2 = BeanUtil.deepCopy(user1, User.class);

        System.out.println("拷贝前 user1: " + user1);
        System.out.println("拷贝后 user2: " + user2);

        // 修改 user1 的引用对象
        user1.getAddress().setCity("Shanghai");
        user1.getTags().add("blogger");

        System.out.println("修改 user1 后:");
        System.out.println("user1: " + user1);
        System.out.println("user2: " + user2);
    }

    public static class Address implements Serializable {
        private String city;

        public Address(String city) {
            this.city = city;
        }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        @Override
        public String toString() {
            return "Address{city='" + city + "'}";
        }
    }

    public static class User implements Serializable {
        private String name;
        private Address address;
        private List<String> tags;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Address getAddress() { return address; }
        public void setAddress(Address address) { this.address = address; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }

        @Override
        public String toString() {
            return "User{name='" + name + "', address=" + address + ", tags=" + tags + "}";
        }

    }
}
