package local.ateng.java.jpa.repository;

import local.ateng.java.jpa.entity.MyUser;
import org.springframework.data.jpa.repository.JpaRepository;

// UserRepository 扩展 JpaRepository
public interface MyUserRepository extends JpaRepository<MyUser, Long> {

}
