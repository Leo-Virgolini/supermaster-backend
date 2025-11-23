package ar.com.leo.super_master_backend.dominio.tipo.repository;

import ar.com.leo.super_master_backend.dominio.tipo.entity.Tipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoRepository extends JpaRepository<Tipo, Integer> {
}