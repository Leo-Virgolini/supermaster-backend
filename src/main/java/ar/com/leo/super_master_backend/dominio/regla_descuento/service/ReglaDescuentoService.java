package ar.com.leo.super_master_backend.dominio.regla_descuento.service;

import ar.com.leo.super_master_backend.dominio.regla_descuento.dto.ReglaDescuentoDTO;

import java.util.List;

public interface ReglaDescuentoService {

    ReglaDescuentoDTO obtener(Integer id);

    List<ReglaDescuentoDTO> listar();

    ReglaDescuentoDTO crear(ReglaDescuentoDTO dto);

    ReglaDescuentoDTO actualizar(Integer id, ReglaDescuentoDTO dto);

    void eliminar(Integer id);

    List<ReglaDescuentoDTO> listarPorCanal(Integer canalId);

    List<ReglaDescuentoDTO> listarActivas(Integer canalId);
}