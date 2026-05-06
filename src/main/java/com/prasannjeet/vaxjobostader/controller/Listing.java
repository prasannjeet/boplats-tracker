package com.prasannjeet.vaxjobostader.controller;

import com.prasannjeet.vaxjobostader.service.HouseSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerErrorException;

@RestController
@RequestMapping("/list")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class Listing {

    private final HouseSyncService houseSyncService;

    @GetMapping("/update")
    public ResponseEntity<String> update() {
        try {
            log.info("Manual list sync triggered via API.");
            houseSyncService.syncHouseList();
            return ResponseEntity.ok("List sync triggered successfully.");
        } catch (Exception e) {
            log.error("Error triggering house list sync", e);
            throw new ServerErrorException("Error triggering sync", e);
        }
    }
}
