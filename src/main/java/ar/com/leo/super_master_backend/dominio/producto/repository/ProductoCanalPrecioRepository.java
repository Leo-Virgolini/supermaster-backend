package ar.com.leo.super_master_backend.dominio.producto.repository;

import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanalPrecio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoCanalPrecioRepository extends JpaRepository<ProductoCanalPrecio, Integer> {

    List<ProductoCanalPrecio> findByProductoId(Integer idProducto);

    List<ProductoCanalPrecio> findByProductoIdOrderByCanalIdAscCuotasAsc(Integer idProducto);

    List<ProductoCanalPrecio> findByCanalId(Integer idCanal);

    @Query("SELECT p FROM ProductoCanalPrecio p WHERE p.canal.id = :canalId AND ((:cuotas IS NULL AND p.cuotas IS NULL) OR p.cuotas = :cuotas)")
    List<ProductoCanalPrecio> findByCanalIdAndCuotas(@Param("canalId") Integer canalId, @Param("cuotas") Integer cuotas);

    Optional<ProductoCanalPrecio> findByProductoIdAndCanalId(Integer productoId, Integer canalId);

    Optional<ProductoCanalPrecio> findByProductoIdAndCanalIdAndCuotas(Integer productoId, Integer canalId, Integer cuotas);

    List<ProductoCanalPrecio> findByProductoIdAndCanalIdOrderByCuotasAsc(Integer productoId, Integer canalId);

    List<ProductoCanalPrecio> findByProductoIdInOrderByProductoIdAscCanalIdAscCuotasAsc(List<Integer> productoIds);

    boolean existsByCanalIdAndCuotas(Integer canalId, Integer cuotas);

    @Query("SELECT DISTINCT p.canal.id FROM ProductoCanalPrecio p WHERE p.producto.id = :productoId")
    List<Integer> findDistinctCanalIdsByProductoId(@Param("productoId") Integer productoId);

}