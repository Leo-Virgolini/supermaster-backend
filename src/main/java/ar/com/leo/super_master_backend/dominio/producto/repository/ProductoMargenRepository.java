package ar.com.leo.super_master_backend.dominio.producto.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoMargen;

@Repository
public interface ProductoMargenRepository extends JpaRepository<ProductoMargen, Integer> {

    Optional<ProductoMargen> findByProductoId(Integer productoId);

    List<ProductoMargen> findByProductoIdIn(List<Integer> productoIds);

    void deleteByProductoId(Integer productoId);

}
