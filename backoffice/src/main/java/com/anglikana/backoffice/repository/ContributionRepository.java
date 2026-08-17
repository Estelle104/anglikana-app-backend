package com.anglikana.backoffice.repository;

import com.anglikana.backoffice.entity.Contribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContributionRepository extends JpaRepository<Contribution, Integer> {
}
