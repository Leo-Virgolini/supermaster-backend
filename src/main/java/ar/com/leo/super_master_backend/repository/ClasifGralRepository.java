package ar.com.leo.super_master_backend.repository;

import ar.com.leo.super_master_backend.entity.ClasifGral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClasifGralRepository extends JpaRepository<ClasifGral, Integer> {
}