package com.prasannjeet.vaxjobostader.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface HouseFloorplanRepository extends JpaRepository<HouseFloorplan, Long> {

    List<HouseFloorplan> findByHouseExternalIdOrderBySortOrderAsc(String houseExternalId);

    List<HouseFloorplan> findByHouseExternalIdInOrderBySortOrderAsc(Collection<String> houseExternalIds);

    @Modifying
    @Query("DELETE FROM HouseFloorplan h WHERE h.houseExternalId = :externalId")
    int deleteByHouseExternalId(@Param("externalId") String externalId);
}
