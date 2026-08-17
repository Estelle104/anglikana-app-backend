package com.anglikana.backoffice.repository;

import com.anglikana.backoffice.entity.Diocese;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DioceseRepository extends JpaRepository<Diocese, Integer> {
}
