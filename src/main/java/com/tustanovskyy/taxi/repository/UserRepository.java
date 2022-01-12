package com.tustanovskyy.taxi.repository;


import com.tustanovskyy.taxi.document.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
//public interface UserRepository extends CrudRepository<User, String> {
public interface UserRepository extends JpaRepository<User, Long> {


    User findByFacebookId(String facebookId);

    User findByPhoneNumber(String phoneNumber);

}
