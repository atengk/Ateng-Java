package local.ateng.java.jpa.repository;

import local.ateng.java.jpa.entity.MyUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// UserRepository 扩展 JpaRepository
public interface MyUserRepository extends JpaRepository<MyUser, Long> {

    @Modifying
    @Query(value = "TRUNCATE TABLE my_user", nativeQuery = true)
    void truncateTable();

    @Query("SELECT u FROM my_user u " +
            "WHERE (:name IS NULL OR :name = '' OR u.name LIKE :name) " +
            "AND (:age IS NULL OR u.age > :age)")
    List<MyUser> findCustomList(@Param("name") String name, @Param("age") Integer age);

    @Query("SELECT u FROM my_user u WHERE u.id = :id")
    MyUser findCustomOne(@Param("id") Long id);
}
