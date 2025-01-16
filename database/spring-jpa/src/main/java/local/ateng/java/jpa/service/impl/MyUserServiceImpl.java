package local.ateng.java.jpa.service.impl;

import local.ateng.java.jpa.entity.MyUser;
import local.ateng.java.jpa.repository.MyUserRepository;
import local.ateng.java.jpa.service.MyUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MyUserServiceImpl implements MyUserService {
    private final MyUserRepository myUserRepository;


    @Override
    public void save(MyUser myUser) {
        MyUser user = myUserRepository.save(myUser);
        System.out.println(user);
    }

    @Override
    public void saveAll(List<MyUser> myUsers) {
        myUserRepository.saveAll(myUsers);
    }

    @Override
    public void update(MyUser myUser) {
        myUserRepository.save(myUser);
    }

    @Override
    public void deleteById(Long id) {
        myUserRepository.deleteById(id);
    }

    @Override
    public List<MyUser> findAll() {
        return myUserRepository.findAll();
    }

    @Override
    public MyUser findById(Long id) {
        return myUserRepository.findById(id).get();
    }

    @Override
    @Transactional
    public void truncate() {
        myUserRepository.truncateTable();
    }

    @Override
    public MyUser findCustomOne(Long id) {
        return myUserRepository.findCustomOne(id);
    }

    @Override
    public List<MyUser> findCustomList(String name, Integer age) {
        return myUserRepository.findCustomList(name, age);
    }
}
