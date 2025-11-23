package ar.com.leo.super_master_backend.dominio.material.repository;

import ar.com.leo.super_master_backend.dominio.material.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Integer> {
}