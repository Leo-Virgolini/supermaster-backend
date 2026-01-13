package ar.com.leo.super_master_backend.dominio.producto.repository;

import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer>, JpaSpecificationExecutor<Producto> {

    Optional<Producto> findBySku(String sku);

    List<Producto> findByMarcaId(Integer idMarca);

    List<Producto> findByOrigenId(Integer idOrigen);

    List<Producto> findByClasifGralId(Integer idClasifGral);

    List<Producto> findByClasifGastroId(Integer idClasifGastro);

    List<Producto> findByTipoId(Integer idTipo);

    List<Producto> findByProveedorId(Integer idProveedor);

    List<Producto> findByMlaId(Integer idMla);

}