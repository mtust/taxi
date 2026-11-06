package com.tustanovskyy.taxi.resources;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AppConfigResourcesTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AppConfigResources(
            "1.0.7", "1.0.8",
            "https://play.google.com/store/apps/details?id=com.hurtom.taxi",
            "https://apps.apple.com/app/id6774705482")).build();

    @Test
    void returnsMinVersionAndStoreUrlPerPlatform() throws Exception {
        mockMvc.perform(get("/app/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minVersion.android").value("1.0.7"))
                .andExpect(jsonPath("$.minVersion.ios").value("1.0.8"))
                .andExpect(jsonPath("$.storeUrl.android").value("https://play.google.com/store/apps/details?id=com.hurtom.taxi"))
                .andExpect(jsonPath("$.storeUrl.ios").value("https://apps.apple.com/app/id6774705482"));
    }
}
