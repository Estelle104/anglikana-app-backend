package com.anglikana.backoffice.repository;

import com.anglikana.backoffice.entity.Ressource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RessourceRepository extends JpaRepository<Ressource, Integer> {
}
