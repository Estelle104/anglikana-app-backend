package com.anglikana.backoffice.repository;

import com.anglikana.backoffice.entity.TypeRessource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TypeRessourceRepository extends JpaRepository<TypeRessource, Integer> {
    List<TypeRessource> findAllByOrderByNomAsc();
}
