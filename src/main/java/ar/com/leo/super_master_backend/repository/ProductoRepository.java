package ar.com.leo.super_master_backend.repository;

import ar.com.leo.super_master_backend.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    Optional<Producto> findBySku(String sku);

    List<Producto> findByMarcaId(Integer idMarca);

    List<Producto> findByOrigenId(Integer idOrigen);

    List<Producto> findByClasifGralId(Integer idClasifGral);

    List<Producto> findByClasifGastroId(Integer idClasifGastro);

    List<Producto> findByTipoId(Integer idTipo);

}