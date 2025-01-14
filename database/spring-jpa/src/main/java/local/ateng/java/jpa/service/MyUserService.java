package local.ateng.java.jpa.service;

import local.ateng.java.jpa.entity.MyUser;

import java.util.List;

public interface MyUserService {

    void save(MyUser myUser);

    void update(MyUser myUser);

    void deleteById(Long id);

    List<MyUser> findAll();

    MyUser findById(Long id);
}

