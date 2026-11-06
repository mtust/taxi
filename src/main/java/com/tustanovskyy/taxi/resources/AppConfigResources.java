package com.tustanovskyy.taxi.resources;

import com.tustanovskyy.taxi.domain.response.AppConfigResponse;
import com.tustanovskyy.taxi.domain.response.AppConfigResponse.PerPlatform;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Forced-update config for the mobile app. Public (see SecurityConfig) because the app checks it
 * before sign-in. Values come from taxi.app.* in application.yml - raise APP_MIN_VERSION_ANDROID /
 * APP_MIN_VERSION_IOS to block older builds; the default 0.0.0 blocks nothing.
 */
@RestController
@RequestMapping("app")
@CrossOrigin(origins = "*")
public class AppConfigResources {

    private final AppConfigResponse config;

    public AppConfigResources(@Value("${taxi.app.min-version.android}") String minVersionAndroid,
                              @Value("${taxi.app.min-version.ios}") String minVersionIos,
                              @Value("${taxi.app.store-url.android}") String storeUrlAndroid,
                              @Value("${taxi.app.store-url.ios}") String storeUrlIos) {
        this.config = new AppConfigResponse(
                new PerPlatform(minVersionAndroid, minVersionIos),
                new PerPlatform(storeUrlAndroid, storeUrlIos));
    }

    @GetMapping("/config")
    public AppConfigResponse getConfig() {
        return config;
    }
}
