package ar.com.leo.super_master_backend.dominio.producto.service;

import ar.com.leo.super_master_backend.dominio.producto.dto.EstadisticasDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.EstadisticasDTO.MargenesPorCuotasDTO;

public interface EstadisticasService {
    EstadisticasDTO obtenerEstadisticas();
    MargenesPorCuotasDTO obtenerMargenesPorCuotas(Integer cuotas);
}
