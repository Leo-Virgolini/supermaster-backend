package ar.com.leo.super_master_backend.dominio.repository;

import ar.com.leo.super_master_backend.dominio.entity.Apto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AptoRepository extends JpaRepository<Apto, Integer> {
}