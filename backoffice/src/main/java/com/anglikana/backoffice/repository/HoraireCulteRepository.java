package com.anglikana.backoffice.repository;

import com.anglikana.backoffice.entity.HoraireCulte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HoraireCulteRepository extends JpaRepository<HoraireCulte, Integer> {
}
