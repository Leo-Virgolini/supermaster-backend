package ar.com.leo.super_master_backend.dominio.producto.service;

import ar.com.leo.super_master_backend.dominio.canal.entity.Canal;
import ar.com.leo.super_master_backend.dominio.producto.calculo.dto.PrecioCalculadoDTO;
import ar.com.leo.super_master_backend.dominio.producto.calculo.service.CalculoPrecioService;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalPrecioDTO;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanalPrecio;
import ar.com.leo.super_master_backend.dominio.producto.mapper.ProductoCanalPrecioMapper;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalPrecioRepository;
import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductoCanalPrecioServiceImpl implements ProductoCanalPrecioService {

    private final ProductoCanalPrecioRepository repo;
    private final ProductoCanalPrecioMapper mapper;
    private final CalculoPrecioService calculoPrecioService;

    @Override
    @Transactional(readOnly = true)
    public ProductoCanalPrecioDTO obtener(Integer productoId, Integer canalId) {

        ProductoCanalPrecio entity = repo.findByProductoIdAndCanalId(productoId, canalId)
                .orElseThrow(() -> new NotFoundException("No hay precio calculado para este producto y canal."));

        return mapper.toDTO(entity);
    }

    @Override
    @Transactional
    public ProductoCanalPrecioDTO recalcular(Integer productoId, Integer canalId) {

        // 1) Calcular el precio en memoria
        PrecioCalculadoDTO calculado = calculoPrecioService.calcularPrecioCanal(productoId, canalId);

        // 2) Buscar registro existente o crear nuevo
        ProductoCanalPrecio entity = repo.findByProductoIdAndCanalId(productoId, canalId)
                .orElseGet(() -> {
                    ProductoCanalPrecio nuevo = new ProductoCanalPrecio();
                    nuevo.setProducto(new Producto(productoId));
                    nuevo.setCanal(new Canal(canalId));
                    return nuevo;
                });

        // 3) Setear valores calculados
        entity.setPvp(calculado.pvp());
        entity.setPvpInflado(calculado.pvpInflado());
        entity.setCostoProducto(calculado.costoProducto());
        entity.setCostosVenta(calculado.costosVenta());
        entity.setIngresoNetoVendedor(calculado.ingresoNetoVendedor());
        entity.setGanancia(calculado.ganancia());
        entity.setMargenPorcentaje(calculado.margenPorcentaje());
        entity.setMarkupPorcentaje(calculado.markupPorcentaje());

        // fecha_ultimo_calculo se actualiza por DB (default CURRENT_TIMESTAMP)

        // 4) Guardar y devolver DTO
        repo.save(entity);

        return mapper.toDTO(entity);
    }

}