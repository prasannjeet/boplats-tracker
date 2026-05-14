package com.prasannjeet.vaxjobostader.controller;

import com.prasannjeet.vaxjobostader.jpa.ObjectType;
import com.prasannjeet.vaxjobostader.jpa.ObjectTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/object-types")
@RequiredArgsConstructor
public class ObjectTypeController {

    private final ObjectTypeRepository objectTypeRepository;

    @GetMapping
    public List<ObjectType> list() {
        return objectTypeRepository.findAll();
    }

    @GetMapping("/{typeId}")
    public ResponseEntity<ObjectType> getOne(@PathVariable String typeId) {
        return objectTypeRepository.findById(typeId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
