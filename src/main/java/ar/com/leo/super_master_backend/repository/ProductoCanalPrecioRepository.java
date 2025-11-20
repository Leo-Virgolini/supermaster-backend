package ar.com.leo.super_master_backend.repository;

import ar.com.leo.super_master_backend.entity.ProductoCanalPrecio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoCanalPrecioRepository extends JpaRepository<ProductoCanalPrecio, Integer> {

    List<ProductoCanalPrecio> findByProductoId(Integer idProducto);

    List<ProductoCanalPrecio> findByCanalId(Integer idCanal);
}