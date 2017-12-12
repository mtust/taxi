package com.tustanovskyy.taxi.repository;


import com.tustanovskyy.taxi.document.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "user")
public interface UserRepositoryRestResources extends CrudRepository<User, String> {


    User findByFacebookId(String facebookId);

}
