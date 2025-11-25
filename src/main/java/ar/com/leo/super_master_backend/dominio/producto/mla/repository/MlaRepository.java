package ar.com.leo.super_master_backend.dominio.producto.mla.repository;

import ar.com.leo.super_master_backend.dominio.producto.mla.entity.Mla;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MlaRepository extends JpaRepository<Mla, Integer> {

    List<Mla> findByProductoId(Integer idProducto);

}