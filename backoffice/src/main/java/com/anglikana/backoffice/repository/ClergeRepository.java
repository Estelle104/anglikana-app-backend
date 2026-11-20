package com.anglikana.backoffice.repository;

import com.anglikana.backoffice.entity.Clerge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClergeRepository extends JpaRepository<Clerge, Integer> {

    /** Clergé actif et son rôle, en une requête (chargement de l'utilisateur connecté). */
    @Query("select c from Clerge c left join fetch c.role where c.id = :id")
    Optional<Clerge> findAvecRoleById(@Param("id") Integer id);
}
