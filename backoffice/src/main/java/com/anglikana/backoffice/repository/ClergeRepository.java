package com.anglikana.backoffice.repository;

import com.anglikana.backoffice.entity.Clerge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClergeRepository extends JpaRepository<Clerge, Integer> {
}
