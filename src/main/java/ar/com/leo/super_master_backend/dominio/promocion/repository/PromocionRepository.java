package ar.com.leo.super_master_backend.dominio.promocion.repository;

import ar.com.leo.super_master_backend.dominio.promocion.entity.Promocion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PromocionRepository extends JpaRepository<Promocion, Integer> {

    Optional<Promocion> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);
}
