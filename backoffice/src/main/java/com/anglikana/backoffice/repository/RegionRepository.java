package com.anglikana.backoffice.repository;

import com.anglikana.backoffice.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegionRepository extends JpaRepository<Region, Integer> {
    List<Region> findByDioceseIdOrderByNomAsc(Integer dioceseId);

    boolean existsByDioceseId(Integer dioceseId);

    boolean existsByResponsableId(Integer responsableId);
}
