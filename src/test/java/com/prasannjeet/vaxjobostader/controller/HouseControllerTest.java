package com.prasannjeet.vaxjobostader.controller;

import com.prasannjeet.vaxjobostader.jpa.HouseFloorplanRepository;
import com.prasannjeet.vaxjobostader.jpa.HouseImageRepository;
import com.prasannjeet.vaxjobostader.jpa.HouseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HouseControllerTest {

    @Mock HouseRepository houseRepository;
    @Mock HouseImageRepository houseImageRepository;
    @Mock HouseFloorplanRepository houseFloorplanRepository;

    @InjectMocks HouseController controller;

    @Test
    void list_defaultsToApplicationDeadlineDisplayFilter() {
        when(houseRepository.findAllDisplayable(any())).thenReturn(List.of());

        controller.list(false);

        ArgumentCaptor<Date> startOfToday = ArgumentCaptor.forClass(Date.class);
        verify(houseRepository).findAllDisplayable(startOfToday.capture());
        verify(houseRepository, never()).findAllByEndDateIsNull();

        Date expected = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        assertThat(startOfToday.getValue()).isEqualTo(expected);
    }

    @Test
    void list_includeEndedReturnsEverything() {
        when(houseRepository.findAll()).thenReturn(List.of());

        controller.list(true);

        verify(houseRepository).findAll();
        verify(houseRepository, never()).findAllDisplayable(any());
        verify(houseRepository, never()).findAllByEndDateIsNull();
    }
}
