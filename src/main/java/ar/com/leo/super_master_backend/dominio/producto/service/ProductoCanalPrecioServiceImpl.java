package ar.com.leo.super_master_backend.dominio.producto.service;

import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalPrecioDTO;
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

    @Override
    @Transactional(readOnly = true)
    public ProductoCanalPrecioDTO obtener(Integer productoId, Integer canalId) {

        ProductoCanalPrecio entity = repo.findByProductoIdAndCanalId(productoId, canalId)
                .orElseThrow(() -> new NotFoundException("No hay precio calculado para este producto y canal."));

        return mapper.toDTO(entity);
    }

}