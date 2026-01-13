package ar.com.leo.super_master_backend.dominio.producto.service;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import ar.com.leo.super_master_backend.dominio.producto.calculo.service.RecalculoPrecioFacade;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoMargenDTO;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoMargen;
import ar.com.leo.super_master_backend.dominio.producto.mapper.ProductoMargenMapper;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoMargenRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductoMargenServiceImpl implements ProductoMargenService {

    private final ProductoMargenRepository repo;
    private final ProductoMargenMapper mapper;
    private final ProductoRepository productoRepository;
    private final RecalculoPrecioFacade recalculoFacade;

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductoMargenDTO> obtener(Integer productoId) {
        return repo.findByProductoId(productoId)
                .map(mapper::toDTO);
    }

    @Override
    @Transactional
    public ProductoMargenDTO guardar(ProductoMargenDTO dto) {
        // Validar que exista el producto
        productoRepository.findById(dto.productoId())
                .orElseThrow(() -> new NotFoundException("Producto no encontrado"));

        // Buscar configuracion existente
        Optional<ProductoMargen> existente = repo.findByProductoId(dto.productoId());

        ProductoMargen pm;
        BigDecimal margenMinoristaAnterior = null;
        BigDecimal margenMayoristaAnterior = null;
        BigDecimal margenFijoMinoristaAnterior = null;
        BigDecimal margenFijoMayoristaAnterior = null;

        if (existente.isPresent()) {
            pm = existente.get();
            // Guardar valores anteriores para detectar cambios
            margenMinoristaAnterior = pm.getMargenMinorista();
            margenMayoristaAnterior = pm.getMargenMayorista();
            margenFijoMinoristaAnterior = pm.getMargenFijoMinorista();
            margenFijoMayoristaAnterior = pm.getMargenFijoMayorista();

            // Actualizar campos
            mapper.updateEntityFromDTO(dto, pm);
        } else {
            pm = new ProductoMargen();
            pm.setProducto(new Producto(dto.productoId()));
            pm.setMargenMinorista(dto.margenMinorista());
            pm.setMargenMayorista(dto.margenMayorista());
            pm.setMargenFijoMinorista(dto.margenFijoMinorista());
            pm.setMargenFijoMayorista(dto.margenFijoMayorista());
            pm.setNotas(dto.notas());
        }

        pm = repo.save(pm);

        // Recalcular si cambió algo que afecta el precio
        boolean cambioMargenMinorista = !Objects.equals(margenMinoristaAnterior, pm.getMargenMinorista());
        boolean cambioMargenMayorista = !Objects.equals(margenMayoristaAnterior, pm.getMargenMayorista());
        boolean cambioMargenFijoMinorista = !Objects.equals(margenFijoMinoristaAnterior, pm.getMargenFijoMinorista());
        boolean cambioMargenFijoMayorista = !Objects.equals(margenFijoMayoristaAnterior, pm.getMargenFijoMayorista());

        if (cambioMargenMinorista || cambioMargenMayorista || cambioMargenFijoMinorista || cambioMargenFijoMayorista) {
            recalculoFacade.recalcularPorCambioProductoMargen(dto.productoId());
        }

        return mapper.toDTO(pm);
    }

    @Override
    @Transactional
    public void eliminar(Integer productoId) {
        repo.deleteByProductoId(productoId);
    }

}
