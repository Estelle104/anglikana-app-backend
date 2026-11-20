package com.anglikana.backoffice.repository;

import com.anglikana.backoffice.entity.District;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DistrictRepository extends JpaRepository<District, Integer> {
    List<District> findByRegionIdOrderByNomAsc(Integer regionId);
    List<District> findByRegionDioceseIdOrderByNomAsc(Integer dioceseId);

    boolean existsByRegionId(Integer regionId);

    boolean existsByResponsableId(Integer responsableId);
}
