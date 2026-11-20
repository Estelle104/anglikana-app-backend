package com.anglikana.backoffice.repository;

import com.anglikana.backoffice.entity.Ressource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RessourceRepository extends JpaRepository<Ressource, Integer> {

    List<Ressource> findByEgliseIdOrderByNomAsc(Integer egliseId);

    /** Ressources d'une église avec leur type et leur responsable, en une seule requête (pas de N+1). */
    @Query("select r from Ressource r join fetch r.type left join fetch r.responsable "
            + "where r.eglise.id = :egliseId order by r.nom")
    List<Ressource> findDetailleesByEgliseId(@Param("egliseId") Integer egliseId);

    boolean existsByEgliseId(Integer egliseId);

    boolean existsByResponsableId(Integer responsableId);
}
