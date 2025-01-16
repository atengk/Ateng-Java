package local.ateng.java.jpa.service;

import local.ateng.java.jpa.entity.MyUser;

import java.util.List;

public interface MyUserService {

    void save(MyUser myUser);

    void saveAll(List<MyUser> myUsers);

    void update(MyUser myUser);

    void deleteById(Long id);

    List<MyUser> findAll();

    MyUser findById(Long id);

    void truncate();

    MyUser findCustomOne(Long id);

    List<MyUser> findCustomList(String name, Integer age);

}

