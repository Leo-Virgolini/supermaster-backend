package ar.com.leo.super_master_backend.dominio.producto.repository;

import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanalPromocion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoCanalPromocionRepository extends JpaRepository<ProductoCanalPromocion, Integer> {

    Optional<ProductoCanalPromocion> findByProductoIdAndCanalId(Integer productoId, Integer canalId);

    List<ProductoCanalPromocion> findByProductoId(Integer productoId);

    List<ProductoCanalPromocion> findByCanalId(Integer canalId);

    List<ProductoCanalPromocion> findByActivaTrue();
}
