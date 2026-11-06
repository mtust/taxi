package com.tustanovskyy.taxi.domain.response;

/**
 * Public, unauthenticated client config fetched by the app on startup/foreground - see
 * AppConfigResources. An app whose own version is below {@code minVersion} for its platform shows a
 * blocking "update the app" screen linking to {@code storeUrl}.
 */
public record AppConfigResponse(PerPlatform minVersion, PerPlatform storeUrl) {

    public record PerPlatform(String android, String ios) {
    }
}
