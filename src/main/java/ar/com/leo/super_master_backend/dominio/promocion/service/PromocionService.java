package ar.com.leo.super_master_backend.dominio.promocion.service;

import ar.com.leo.super_master_backend.dominio.promocion.dto.PromocionCreateDTO;
import ar.com.leo.super_master_backend.dominio.promocion.dto.PromocionDTO;
import ar.com.leo.super_master_backend.dominio.promocion.dto.PromocionUpdateDTO;

import java.util.List;

public interface PromocionService {

    List<PromocionDTO> listar();

    PromocionDTO obtenerPorId(Integer id);

    PromocionDTO obtenerPorCodigo(String codigo);

    PromocionDTO crear(PromocionCreateDTO dto);

    PromocionDTO actualizar(Integer id, PromocionUpdateDTO dto);

    void eliminar(Integer id);
}
