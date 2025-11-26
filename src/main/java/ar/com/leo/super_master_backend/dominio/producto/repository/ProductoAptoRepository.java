package ar.com.leo.super_master_backend.dominio.producto.repository;

import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoApto;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoAptoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoAptoRepository extends JpaRepository<ProductoApto, ProductoAptoId> {

    List<ProductoApto> findByProductoId(Integer productoId);

    List<ProductoApto> findByAptoId(Integer aptoId);

    void deleteByProductoIdAndAptoId(Integer productoId, Integer aptoId);
}