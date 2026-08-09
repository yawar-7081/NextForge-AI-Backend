package com.yawar.nextforgeai.repository;

import com.yawar.nextforgeai.entity.Plan;
import io.micrometer.observation.ObservationFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan,String> {

    Optional<Plan> findByName(String free);

    Optional<Plan> findByStripePriceId(String id);
}
