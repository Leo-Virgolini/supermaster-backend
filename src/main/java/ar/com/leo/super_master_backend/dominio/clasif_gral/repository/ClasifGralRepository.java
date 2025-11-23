package ar.com.leo.super_master_backend.dominio.clasif_gral.repository;

import ar.com.leo.super_master_backend.dominio.clasif_gral.entity.ClasifGral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClasifGralRepository extends JpaRepository<ClasifGral, Integer> {
}