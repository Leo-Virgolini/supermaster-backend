package ar.com.leo.super_master_backend.dominio.producto.service;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import ar.com.leo.super_master_backend.dominio.producto.calculo.service.RecalculoPrecioFacade;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoCanalDTO;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanal;
import ar.com.leo.super_master_backend.dominio.producto.mapper.ProductoCanalMapper;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductoCanalServiceImpl implements ProductoCanalService {

    private final ProductoCanalRepository repo;
    private final ProductoCanalMapper mapper;
    private final ProductoRepository productoRepository;
    private final RecalculoPrecioFacade recalculoFacade;

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductoCanalDTO> obtener(Integer productoId) {
        return repo.findByProductoId(productoId)
                .map(mapper::toDTO);
    }

    @Override
    @Transactional
    public ProductoCanalDTO guardar(ProductoCanalDTO dto) {
        // Validar que exista el producto
        productoRepository.findById(dto.productoId())
                .orElseThrow(() -> new NotFoundException("Producto no encontrado"));

        // Buscar configuracion existente
        Optional<ProductoCanal> existente = repo.findByProductoId(dto.productoId());

        ProductoCanal pc;
        BigDecimal margenMinoristaAnterior = null;
        BigDecimal margenMayoristaAnterior = null;
        BigDecimal margenFijoMinoristaAnterior = null;
        BigDecimal margenFijoMayoristaAnterior = null;

        if (existente.isPresent()) {
            pc = existente.get();
            // Guardar valores anteriores para detectar cambios
            margenMinoristaAnterior = pc.getMargenMinorista();
            margenMayoristaAnterior = pc.getMargenMayorista();
            margenFijoMinoristaAnterior = pc.getMargenFijoMinorista();
            margenFijoMayoristaAnterior = pc.getMargenFijoMayorista();

            // Actualizar campos
            mapper.updateEntityFromDTO(dto, pc);
        } else {
            pc = new ProductoCanal();
            pc.setProducto(new Producto(dto.productoId()));
            pc.setMargenMinorista(dto.margenMinorista());
            pc.setMargenMayorista(dto.margenMayorista());
            pc.setMargenFijoMinorista(dto.margenFijoMinorista());
            pc.setMargenFijoMayorista(dto.margenFijoMayorista());
            pc.setNotas(dto.notas());
        }

        pc = repo.save(pc);

        // Recalcular si cambió algo que afecta el precio
        boolean cambioMargenMinorista = !Objects.equals(margenMinoristaAnterior, pc.getMargenMinorista());
        boolean cambioMargenMayorista = !Objects.equals(margenMayoristaAnterior, pc.getMargenMayorista());
        boolean cambioMargenFijoMinorista = !Objects.equals(margenFijoMinoristaAnterior, pc.getMargenFijoMinorista());
        boolean cambioMargenFijoMayorista = !Objects.equals(margenFijoMayoristaAnterior, pc.getMargenFijoMayorista());

        if (cambioMargenMinorista || cambioMargenMayorista || cambioMargenFijoMinorista || cambioMargenFijoMayorista) {
            recalculoFacade.recalcularPorCambioProductoCanal(dto.productoId());
        }

        return mapper.toDTO(pc);
    }

    @Override
    @Transactional
    public void eliminar(Integer productoId) {
        repo.deleteByProductoId(productoId);
    }

}
