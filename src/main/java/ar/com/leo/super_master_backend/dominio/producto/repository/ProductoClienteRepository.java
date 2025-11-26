package ar.com.leo.super_master_backend.dominio.producto.repository;

import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCliente;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoClienteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoClienteRepository extends JpaRepository<ProductoCliente, ProductoClienteId> {

    List<ProductoCliente> findByClienteId(Integer idCliente);

    List<ProductoCliente> findByProductoId(Integer idProducto);

    void deleteByProductoIdAndClienteId(Integer productoId, Integer clienteId);

}