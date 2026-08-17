package com.anglikana.backoffice.repository;

import com.anglikana.backoffice.entity.Eglise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EgliseRepository extends JpaRepository<Eglise, Integer> {
}
