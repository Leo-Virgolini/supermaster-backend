package ar.com.leo.super_master_backend.dominio.proveedor.repository;

import ar.com.leo.super_master_backend.dominio.proveedor.entity.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Integer> {
}