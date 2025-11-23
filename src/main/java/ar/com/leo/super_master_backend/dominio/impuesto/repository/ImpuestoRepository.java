package ar.com.leo.super_master_backend.dominio.impuesto.repository;

import ar.com.leo.super_master_backend.dominio.impuesto.entity.Impuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImpuestoRepository extends JpaRepository<Impuesto, Integer> {
}