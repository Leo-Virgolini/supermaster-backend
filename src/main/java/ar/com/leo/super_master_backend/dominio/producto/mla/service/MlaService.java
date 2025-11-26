package ar.com.leo.super_master_backend.dominio.producto.mla.service;

import ar.com.leo.super_master_backend.dominio.producto.mla.dto.MlaDTO;

import java.util.List;

public interface MlaService {

    List<MlaDTO> listarPorProducto(Integer productoId);

    MlaDTO crear(Integer productoId, MlaDTO dto);

    MlaDTO actualizar(Integer productoId, Integer mlaId, MlaDTO dto);

    void eliminar(Integer productoId, Integer mlaId);
}