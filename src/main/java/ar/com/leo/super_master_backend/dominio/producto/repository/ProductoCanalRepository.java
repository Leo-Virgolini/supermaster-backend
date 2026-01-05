package ar.com.leo.super_master_backend.dominio.producto.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanal;

@Repository
public interface ProductoCanalRepository extends JpaRepository<ProductoCanal, Integer> {

    List<ProductoCanal> findByProductoId(Integer productoId);

    Optional<ProductoCanal> findByProductoIdAndCanalId(Integer productoId, Integer canalId);

    List<ProductoCanal> findByCanalId(Integer canalId);

    void deleteByProductoIdAndCanalId(Integer productoId, Integer canalId);
}