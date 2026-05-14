package com.prasannjeet.vaxjobostader.controller;

import com.prasannjeet.vaxjobostader.jpa.ObjectType;
import com.prasannjeet.vaxjobostader.jpa.ObjectTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObjectTypeControllerTest {

    @Mock ObjectTypeRepository repository;
    @InjectMocks ObjectTypeController controller;

    @Test
    void list_returnsAllObjectTypes() {
        ObjectType type = new ObjectType(
            "residential", "Bostad", null, null, null, null, null, null, null, 169, null);
        when(repository.findAll()).thenReturn(List.of(type));

        List<ObjectType> result = controller.list();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTypeId()).isEqualTo("residential");
        assertThat(result.get(0).getDisplayName()).isEqualTo("Bostad");
        assertThat(result.get(0).getNumberOfMarketObjects()).isEqualTo(169);
    }

    @Test
    void getOne_unknownTypeId_returns404() {
        when(repository.findById("unknown")).thenReturn(Optional.empty());

        ResponseEntity<ObjectType> response = controller.getOne("unknown");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getOne_knownTypeId_returnsType() {
        ObjectType type = new ObjectType(
            "parking", "Parkering", null, null, null, null, null, null, null, 831, null);
        when(repository.findById("parking")).thenReturn(Optional.of(type));

        ResponseEntity<ObjectType> response = controller.getOne("parking");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTypeId()).isEqualTo("parking");
        assertThat(response.getBody().getDisplayName()).isEqualTo("Parkering");
    }
}
