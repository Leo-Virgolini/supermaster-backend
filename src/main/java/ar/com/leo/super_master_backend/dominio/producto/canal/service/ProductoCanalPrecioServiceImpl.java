package ar.com.leo.super_master_backend.dominio.producto.canal.service;

import ar.com.leo.super_master_backend.dominio.producto.calculo.dto.PrecioCalculadoDTO;
import ar.com.leo.super_master_backend.dominio.producto.calculo.service.CalculoPrecioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductoCanalPrecioServiceImpl implements ProductoCanalPrecioService {

    private final CalculoPrecioService calculoPrecioService;

    @Override
    public PrecioCalculadoDTO recalcular(Integer idProducto, Integer idCanal) {
        return calculoPrecioService.recalcularYGuardar(idProducto, idCanal);
    }

}