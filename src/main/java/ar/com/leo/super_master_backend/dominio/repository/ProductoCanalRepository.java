package ar.com.leo.super_master_backend.dominio.repository;

import ar.com.leo.super_master_backend.dominio.entity.ProductoCanal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoCanalRepository extends JpaRepository<ProductoCanal, Integer> {

    List<ProductoCanal> findByProductoId(Integer idProducto);

    List<ProductoCanal> findByCanalId(Integer idCanal);
}