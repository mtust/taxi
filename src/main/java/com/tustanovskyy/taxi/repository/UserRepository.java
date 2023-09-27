package com.tustanovskyy.taxi.repository;


import com.tustanovskyy.taxi.document.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
//public interface UserRepository extends CrudRepository<User, String> {
public interface UserRepository extends MongoRepository<User, Long> {


    User findByFacebookId(String facebookId);

    User findByPhoneNumber(String phoneNumber);

}
