package ar.com.leo.super_master_backend.dominio.canal.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.leo.super_master_backend.dominio.canal.dto.CanalCreateDTO;
import ar.com.leo.super_master_backend.dominio.canal.dto.CanalDTO;
import ar.com.leo.super_master_backend.dominio.canal.dto.CanalUpdateDTO;
import ar.com.leo.super_master_backend.dominio.canal.entity.Canal;
import ar.com.leo.super_master_backend.dominio.canal.entity.TipoCanal;
import ar.com.leo.super_master_backend.dominio.canal.mapper.CanalMapper;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalRepository;
import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import ar.com.leo.super_master_backend.dominio.producto.calculo.service.RecalculoPrecioFacade;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanalPrecio;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalPrecioRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CanalServiceImpl implements CanalService {

    private final CanalRepository canalRepository;
    private final CanalMapper canalMapper;

    private final ProductoCanalRepository productoCanalRepository;
    private final ProductoCanalPrecioRepository productoCanalPrecioRepository;
    private final RecalculoPrecioFacade recalculoFacade;

    // =======================================
    // CRUD + DTOs
    // =======================================
    @Override
    @Transactional(readOnly = true)
    public Page<CanalDTO> listar(Pageable pageable) {
        return canalRepository.findAll(pageable)
                .map(canalMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public CanalDTO obtener(Integer id) {
        return canalRepository.findById(id)
                .map(canalMapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Canal no encontrado"));
    }

    @Override
    @Transactional
    public CanalDTO crear(CanalCreateDTO dto) {
        Canal entity = canalMapper.toEntity(dto);
        canalRepository.save(entity);
        return canalMapper.toDTO(entity);
    }

    @Override
    @Transactional
    public CanalDTO actualizar(Integer id, CanalUpdateDTO dto) {
        Canal entity = canalRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Canal no encontrado"));

        canalMapper.updateEntityFromDTO(dto, entity);
        canalRepository.save(entity);

        return canalMapper.toDTO(entity);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        if (!canalRepository.existsById(id)) {
            throw new NotFoundException("Canal no encontrado");
        }
        canalRepository.deleteById(id);
    }

    // ===================================================
    // LÓGICA DE NEGOCIO: CAMBIO DE MARGEN DEL CANAL
    // ===================================================
    @Override
    @Transactional
    public void actualizarMargen(Integer idCanal, BigDecimal nuevoMargen) {

        // 0) Validar canal y obtener su tipo
        Canal canal = canalRepository.findById(idCanal)
                .orElseThrow(() -> new NotFoundException("Canal no encontrado"));

        TipoCanal tipoCanal = canal.getTipoCanal() != null ? canal.getTipoCanal() : TipoCanal.MINORISTA;

        // 1) Obtener todos los productos que tienen precios calculados para este canal
        List<ProductoCanalPrecio> preciosCanal = productoCanalPrecioRepository.findByCanalId(idCanal);

        // 2) Actualizar margen de cada producto según el tipo de canal
        preciosCanal.stream()
                .map(precio -> precio.getProducto().getId())
                .distinct()
                .forEach(productoId -> {
                    productoCanalRepository.findByProductoId(productoId)
                            .ifPresent(productoCanal -> {
                                if (tipoCanal == TipoCanal.MAYORISTA) {
                                    productoCanal.setMargenMayorista(nuevoMargen);
                                } else {
                                    productoCanal.setMargenMinorista(nuevoMargen);
                                }
                                productoCanalRepository.save(productoCanal);
                            });
                });

        // 3) Recalcular precios de todos los productos del canal
        recalculoFacade.recalcularTodosProductosDelCanal(idCanal);
    }

}
