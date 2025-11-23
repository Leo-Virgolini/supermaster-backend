package ar.com.leo.super_master_backend.dominio.repository;

import ar.com.leo.super_master_backend.dominio.entity.ProductoCatalogo;
import ar.com.leo.super_master_backend.dominio.entity.ProductoCatalogoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoCatalogoRepository extends JpaRepository<ProductoCatalogo, ProductoCatalogoId> {

    List<ProductoCatalogo> findByCatalogoId(Integer idCatalogo);

    List<ProductoCatalogo> findByProductoId(Integer idProducto);
}