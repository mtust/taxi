package com.tustanovskyy.taxi.service;

import com.tustanovskyy.taxi.document.User;

import java.util.Collection;

public interface UserService {

    void createRideFromFacebook(String userFacebookId, String text) throws Exception;

    Collection<User> findPartners(User user);

    Collection<User> findPartners(String userId);

    User createUser(User user);

    User findUser(String userId);
}
