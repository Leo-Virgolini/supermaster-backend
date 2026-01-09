package ar.com.leo.super_master_backend.dominio.producto.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanal;

@Repository
public interface ProductoCanalRepository extends JpaRepository<ProductoCanal, Integer> {

    Optional<ProductoCanal> findByProductoId(Integer productoId);

    void deleteByProductoId(Integer productoId);

}
