package ar.com.leo.super_master_backend.dominio.producto.repository;

import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoCanalRepository extends JpaRepository<ProductoCanal, Integer> {

    List<ProductoCanal> findByProductoId(Integer idProducto);

    List<ProductoCanal> findByCanalId(Integer idCanal);

    Optional<ProductoCanal> findByIdProductoIdAndIdCanalId(Integer idProducto, Integer idCanal);

}