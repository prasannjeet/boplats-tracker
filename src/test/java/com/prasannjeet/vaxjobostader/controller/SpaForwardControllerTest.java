package com.prasannjeet.vaxjobostader.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SpaForwardControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void listingDeepLinkForwardsToIndex() throws Exception {
        mvc.perform(get("/listings/2137"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void listingsRootForwardsToIndex() throws Exception {
        mvc.perform(get("/listings"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void savedForwardsToIndex() throws Exception {
        mvc.perform(get("/saved"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void typeDetailForwardsToIndex() throws Exception {
        mvc.perform(get("/types/residential"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void unknownPathStillNotFound() throws Exception {
        mvc.perform(get("/definitely-not-a-route"))
            .andExpect(status().isNotFound());
    }
}
