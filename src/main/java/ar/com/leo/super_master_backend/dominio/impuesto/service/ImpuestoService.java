package ar.com.leo.super_master_backend.dominio.impuesto.service;

import ar.com.leo.super_master_backend.dominio.impuesto.dto.ImpuestoDTO;

import java.util.List;

public interface ImpuestoService {
    List<ImpuestoDTO> listar();

    ImpuestoDTO obtener(Integer id);
}