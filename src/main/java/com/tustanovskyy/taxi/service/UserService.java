package com.tustanovskyy.taxi.service;

import com.google.maps.errors.ApiException;

import java.io.IOException;

public interface UserService {

    void createRideFromFacebook(String userFacebookId, String text) throws Exception;


}
