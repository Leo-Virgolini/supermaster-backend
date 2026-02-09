package ar.com.leo.super_master_backend.dominio.producto.repository;

import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer>, JpaSpecificationExecutor<Producto> {

    Optional<Producto> findBySku(String sku);

    /**
     * Obtiene un producto con sus relaciones necesarias para evaluar reglas de canal_concepto_regla.
     * Usa LEFT JOIN FETCH para cargar marca, clasifGral, clasifGastro y tipo de forma eager.
     */
    @Query("SELECT p FROM Producto p " +
           "LEFT JOIN FETCH p.marca " +
           "LEFT JOIN FETCH p.clasifGral " +
           "LEFT JOIN FETCH p.clasifGastro " +
           "LEFT JOIN FETCH p.tipo " +
           "WHERE p.id = :id")
    Optional<Producto> findByIdConRelacionesParaReglas(Integer id);

    List<Producto> findByMarcaId(Integer marcaId);

    List<Producto> findByOrigenId(Integer origenId);

    List<Producto> findByClasifGralId(Integer clasifGralId);

    List<Producto> findByClasifGastroId(Integer clasifGastroId);

    List<Producto> findByTipoId(Integer tipoId);

    List<Producto> findByProveedorId(Integer proveedorId);

    List<Producto> findByMlaId(Integer mlaId);

    List<Producto> findByMaterialId(Integer materialId);

}