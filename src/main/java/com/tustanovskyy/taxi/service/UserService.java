package com.tustanovskyy.taxi.service;

import com.tustanovskyy.taxi.document.User;

import java.util.Collection;

public interface UserService {

    void createRideFromFacebook(String userFacebookId, String text) throws Exception;

    Collection<User> findPartners(User user);

    User createUser(User user);
}
