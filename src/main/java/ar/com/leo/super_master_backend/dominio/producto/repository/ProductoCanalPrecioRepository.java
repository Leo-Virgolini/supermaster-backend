package ar.com.leo.super_master_backend.dominio.producto.repository;

import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanalPrecio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoCanalPrecioRepository extends JpaRepository<ProductoCanalPrecio, Integer> {

    List<ProductoCanalPrecio> findByProductoId(Integer idProducto);

    List<ProductoCanalPrecio> findByProductoIdOrderByCanalIdAscCuotasAsc(Integer idProducto);

    List<ProductoCanalPrecio> findByCanalId(Integer idCanal);

    Optional<ProductoCanalPrecio> findByProductoIdAndCanalId(Integer productoId, Integer canalId);

    Optional<ProductoCanalPrecio> findByProductoIdAndCanalIdAndCuotas(Integer productoId, Integer canalId, Integer cuotas);

    List<ProductoCanalPrecio> findByProductoIdAndCanalIdOrderByCuotasAsc(Integer productoId, Integer canalId);

    List<ProductoCanalPrecio> findByProductoIdInOrderByProductoIdAscCanalIdAscCuotasAsc(List<Integer> productoIds);

}