package ar.com.leo.super_master_backend.dominio.canal.repository;

import ar.com.leo.super_master_backend.dominio.canal.entity.Canal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CanalRepository extends JpaRepository<Canal, Integer> {
}