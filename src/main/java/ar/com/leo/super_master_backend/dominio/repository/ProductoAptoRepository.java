package ar.com.leo.super_master_backend.dominio.repository;

import ar.com.leo.super_master_backend.dominio.entity.ProductoApto;
import ar.com.leo.super_master_backend.dominio.entity.ProductoAptoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoAptoRepository extends JpaRepository<ProductoApto, ProductoAptoId> {

    List<ProductoApto> findByAptoId(Integer idApto);

    List<ProductoApto> findByProductoId(Integer idProducto);
}