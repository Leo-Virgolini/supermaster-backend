package ar.com.leo.super_master_backend.dominio.producto.repository;

import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanalPrecioInflado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoCanalPrecioInfladoRepository extends JpaRepository<ProductoCanalPrecioInflado, Integer> {

    Optional<ProductoCanalPrecioInflado> findByProductoIdAndCanalId(Integer productoId, Integer canalId);

    List<ProductoCanalPrecioInflado> findByProductoId(Integer productoId);

    List<ProductoCanalPrecioInflado> findByCanalId(Integer canalId);

    List<ProductoCanalPrecioInflado> findByActivaTrue();
}
