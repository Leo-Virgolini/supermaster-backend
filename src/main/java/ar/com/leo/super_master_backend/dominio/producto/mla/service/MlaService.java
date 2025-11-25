package ar.com.leo.super_master_backend.dominio.producto.mla.service;

import ar.com.leo.super_master_backend.dominio.producto.mla.dto.MlaDTO;

import java.util.List;

public interface MlaService {

    MlaDTO obtener(Integer id);

    List<MlaDTO> listarPorProducto(Integer productoId);

    MlaDTO crear(MlaDTO dto);

    MlaDTO actualizar(Integer id, MlaDTO dto);

    void eliminar(Integer id);
}