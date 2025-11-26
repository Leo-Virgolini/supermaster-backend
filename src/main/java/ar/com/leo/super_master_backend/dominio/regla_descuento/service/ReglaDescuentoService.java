package ar.com.leo.super_master_backend.dominio.regla_descuento.service;

import ar.com.leo.super_master_backend.dominio.regla_descuento.dto.ReglaDescuentoCreateDTO;
import ar.com.leo.super_master_backend.dominio.regla_descuento.dto.ReglaDescuentoDTO;
import ar.com.leo.super_master_backend.dominio.regla_descuento.dto.ReglaDescuentoUpdateDTO;

import java.util.List;

public interface ReglaDescuentoService {

    List<ReglaDescuentoDTO> listar();

    List<ReglaDescuentoDTO> listarPorCanal(Integer canalId);

    ReglaDescuentoDTO obtener(Integer id);

    ReglaDescuentoDTO crear(ReglaDescuentoCreateDTO dto);

    ReglaDescuentoDTO actualizar(Integer id, ReglaDescuentoUpdateDTO dto);

    void eliminar(Integer id);
}