package ar.com.leo.super_master_backend.dominio.producto.repository;

import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCatalogo;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCatalogoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoCatalogoRepository extends JpaRepository<ProductoCatalogo, ProductoCatalogoId> {

    List<ProductoCatalogo> findByCatalogoId(Integer catalogoId);

    List<ProductoCatalogo> findByProductoId(Integer productoId);

    void deleteByProductoIdAndCatalogoId(Integer productoId, Integer catalogoId);

}