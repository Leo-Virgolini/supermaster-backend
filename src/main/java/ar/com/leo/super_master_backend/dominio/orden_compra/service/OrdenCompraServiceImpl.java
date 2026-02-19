package ar.com.leo.super_master_backend.dominio.orden_compra.service;

import ar.com.leo.super_master_backend.dominio.common.exception.BadRequestException;
import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import ar.com.leo.super_master_backend.dominio.orden_compra.dto.*;
import ar.com.leo.super_master_backend.dominio.orden_compra.entity.EstadoOrdenCompra;
import ar.com.leo.super_master_backend.dominio.orden_compra.entity.OrdenCompra;
import ar.com.leo.super_master_backend.dominio.orden_compra.entity.OrdenCompraLinea;
import ar.com.leo.super_master_backend.dominio.orden_compra.mapper.OrdenCompraMapper;
import ar.com.leo.super_master_backend.dominio.orden_compra.repository.OrdenCompraLineaRepository;
import ar.com.leo.super_master_backend.dominio.orden_compra.repository.OrdenCompraRepository;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoRepository;
import ar.com.leo.super_master_backend.dominio.proveedor.entity.Proveedor;
import ar.com.leo.super_master_backend.dominio.proveedor.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ar.com.leo.super_master_backend.dominio.orden_compra.entity.EstadoOrdenCompra.*;

@Service
@RequiredArgsConstructor
public class OrdenCompraServiceImpl implements OrdenCompraService {

    private final OrdenCompraRepository ordenCompraRepository;
    private final OrdenCompraLineaRepository ordenCompraLineaRepository;
    private final OrdenCompraMapper ordenCompraMapper;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<OrdenCompraDTO> listar(Pageable pageable, Integer proveedorId, EstadoOrdenCompra estado) {
        Page<OrdenCompra> page;
        if (proveedorId != null && estado != null) {
            page = ordenCompraRepository.findByProveedorIdAndEstado(proveedorId, estado, pageable);
        } else if (proveedorId != null) {
            page = ordenCompraRepository.findByProveedorId(proveedorId, pageable);
        } else if (estado != null) {
            page = ordenCompraRepository.findByEstado(estado, pageable);
        } else {
            page = ordenCompraRepository.findAll(pageable);
        }
        return page.map(ordenCompraMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public OrdenCompraDTO obtener(Integer id) {
        OrdenCompra oc = ordenCompraRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Orden de compra no encontrada"));
        return ordenCompraMapper.toDTO(oc);
    }

    @Override
    @Transactional
    public OrdenCompraDTO crear(OrdenCompraCreateDTO dto) {
        Proveedor proveedor = proveedorRepository.findById(dto.proveedorId())
                .orElseThrow(() -> new NotFoundException("Proveedor no encontrado"));

        OrdenCompra oc = new OrdenCompra();
        oc.setProveedor(proveedor);
        oc.setEstado(EstadoOrdenCompra.BORRADOR);
        oc.setObservaciones(dto.observaciones());

        for (OrdenCompraLineaCreateDTO lineaDto : dto.lineas()) {
            Producto producto = productoRepository.findById(lineaDto.productoId())
                    .orElseThrow(() -> new NotFoundException("Producto no encontrado: ID " + lineaDto.productoId()));

            OrdenCompraLinea linea = new OrdenCompraLinea();
            linea.setOrdenCompra(oc);
            linea.setProducto(producto);
            linea.setCantidadPedida(lineaDto.cantidadPedida());
            linea.setCantidadRecibida(0);
            linea.setCostoUnitario(lineaDto.costoUnitario());
            oc.getLineas().add(linea);
        }

        ordenCompraRepository.save(oc);
        return ordenCompraMapper.toDTO(oc);
    }

    @Override
    @Transactional
    public OrdenCompraDTO actualizar(Integer id, OrdenCompraUpdateDTO dto) {
        OrdenCompra oc = ordenCompraRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Orden de compra no encontrada"));

        if (oc.getEstado() != EstadoOrdenCompra.BORRADOR) {
            throw new BadRequestException("Solo se pueden editar órdenes en estado BORRADOR");
        }

        if (dto.observaciones() != null) {
            oc.setObservaciones(dto.observaciones());
        }

        // Si se envían líneas, reemplazar todas las existentes
        if (dto.lineas() != null) {
            if (dto.lineas().isEmpty()) {
                throw new BadRequestException("La orden debe tener al menos una línea");
            }

            oc.getLineas().clear();
            for (OrdenCompraLineaCreateDTO lineaDto : dto.lineas()) {
                Producto producto = productoRepository.findById(lineaDto.productoId())
                        .orElseThrow(() -> new NotFoundException("Producto no encontrado: ID " + lineaDto.productoId()));

                OrdenCompraLinea linea = new OrdenCompraLinea();
                linea.setOrdenCompra(oc);
                linea.setProducto(producto);
                linea.setCantidadPedida(lineaDto.cantidadPedida());
                linea.setCantidadRecibida(0);
                linea.setCostoUnitario(lineaDto.costoUnitario());
                oc.getLineas().add(linea);
            }
        }

        ordenCompraRepository.save(oc);
        return ordenCompraMapper.toDTO(oc);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        OrdenCompra oc = ordenCompraRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Orden de compra no encontrada"));

        if (oc.getEstado() != EstadoOrdenCompra.BORRADOR) {
            throw new BadRequestException("Solo se pueden eliminar órdenes en estado BORRADOR");
        }

        ordenCompraRepository.delete(oc);
    }

    @Override
    @Transactional
    public OrdenCompraDTO enviar(Integer id) {
        OrdenCompra oc = ordenCompraRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Orden de compra no encontrada"));

        if (oc.getEstado() != EstadoOrdenCompra.BORRADOR) {
            throw new BadRequestException("Solo se pueden enviar órdenes en estado BORRADOR");
        }

        oc.setEstado(EstadoOrdenCompra.ENVIADA);
        ordenCompraRepository.save(oc);
        return ordenCompraMapper.toDTO(oc);
    }

    @Override
    @Transactional
    public OrdenCompraDTO registrarRecepcion(Integer id, RecepcionDTO dto) {
        OrdenCompra oc = ordenCompraRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Orden de compra no encontrada"));

        if (oc.getEstado() != EstadoOrdenCompra.ENVIADA && oc.getEstado() != EstadoOrdenCompra.RECIBIDA_PARCIAL) {
            throw new BadRequestException("Solo se puede registrar recepción en órdenes ENVIADA o RECIBIDA_PARCIAL");
        }

        // Crear mapa de líneas por ID para acceso rápido
        Map<Integer, OrdenCompraLinea> lineasMap = new HashMap<>();
        for (OrdenCompraLinea linea : oc.getLineas()) {
            lineasMap.put(linea.getId(), linea);
        }

        for (RecepcionDTO.LineaRecepcionDTO lineaRecepcion : dto.lineas()) {
            OrdenCompraLinea linea = lineasMap.get(lineaRecepcion.lineaId());
            if (linea == null) {
                throw new NotFoundException("Línea no encontrada: ID " + lineaRecepcion.lineaId());
            }

            int nuevaCantidad = lineaRecepcion.cantidadRecibida();
            if (nuevaCantidad > linea.getCantidadPedida()) {
                throw new BadRequestException(
                        String.format("La cantidad recibida (%d) no puede exceder la pedida (%d) para la línea %d",
                                nuevaCantidad, linea.getCantidadPedida(), linea.getId()));
            }

            linea.setCantidadRecibida(nuevaCantidad);
        }

        // Determinar nuevo estado
        boolean todasCompletas = oc.getLineas().stream()
                .allMatch(l -> l.getCantidadRecibida() >= l.getCantidadPedida());
        boolean algunaRecibida = oc.getLineas().stream()
                .anyMatch(l -> l.getCantidadRecibida() > 0);

        if (todasCompletas) {
            oc.setEstado(EstadoOrdenCompra.COMPLETA);
        } else if (algunaRecibida) {
            oc.setEstado(EstadoOrdenCompra.RECIBIDA_PARCIAL);
        }

        ordenCompraRepository.save(oc);
        return ordenCompraMapper.toDTO(oc);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, Integer> obtenerPendientesPorProducto() {
        List<Object[]> resultados = ordenCompraLineaRepository.findPendientesPorProducto(
                List.of(ENVIADA, RECIBIDA_PARCIAL));
        Map<Integer, Integer> pendientes = new HashMap<>();
        for (Object[] row : resultados) {
            Integer productoId = (Integer) row[0];
            Long cantidad = (Long) row[1];
            pendientes.put(productoId, cantidad.intValue());
        }
        return pendientes;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, UltimaCompra> obtenerUltimaCompraPorProducto() {
        List<Object[]> resultados = ordenCompraLineaRepository.findUltimasCompras(
                List.of(BORRADOR, CANCELADA));
        // Ordenados por fecha DESC, tomo solo la primera ocurrencia por producto
        Map<Integer, UltimaCompra> ultimaCompra = new HashMap<>();
        for (Object[] row : resultados) {
            Integer productoId = (Integer) row[0];
            if (!ultimaCompra.containsKey(productoId)) {
                LocalDateTime fecha = (LocalDateTime) row[1];
                Integer cantidad = (Integer) row[2];
                ultimaCompra.put(productoId, new UltimaCompra(fecha, cantidad));
            }
        }
        return ultimaCompra;
    }
}
