package com.prasannjeet.vaxjobostader.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The Vue SPA uses HTML5 history routing (createWebHistory), so a hard
 * refresh or shared deep link like /listings/2137 must serve index.html
 * and let the client router take over. The route list is explicit and
 * mirrors frontend/src/router/index.ts on purpose: a catch-all would
 * swallow genuinely unknown paths that should stay 404.
 */
@Controller
public class SpaForwardController {

    @GetMapping({"/listings", "/listings/{id}", "/saved", "/types/{typeId}"})
    public String forwardSpaRoutes() {
        return "forward:/index.html";
    }
}
