package ar.com.leo.super_master_backend.repository;

import ar.com.leo.super_master_backend.entity.ReglaDescuento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReglaDescuentoRepository extends JpaRepository<ReglaDescuento, Integer> {

    List<ReglaDescuento> findByCanalId(Integer idCanal);

    List<ReglaDescuento> findByClasifGralId(Integer idClasifGral);

    List<ReglaDescuento> findByClasifGastroId(Integer idClasifGastro);

    List<ReglaDescuento> findByCatalogoId(Integer idCatalogo);
}