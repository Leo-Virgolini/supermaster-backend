package ar.com.leo.super_master_backend.dominio.canal.service;

import ar.com.leo.super_master_backend.dominio.canal.dto.CanalCreateDTO;
import ar.com.leo.super_master_backend.dominio.canal.dto.CanalDTO;
import ar.com.leo.super_master_backend.dominio.canal.dto.CanalUpdateDTO;
import ar.com.leo.super_master_backend.dominio.canal.entity.Canal;
import ar.com.leo.super_master_backend.dominio.canal.mapper.CanalMapper;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalRepository;
import ar.com.leo.super_master_backend.dominio.producto.calculo.service.CalculoPrecioService;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanal;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalRepository;
import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CanalServiceImpl implements CanalService {

    private final CanalRepository canalRepository;
    private final CanalMapper canalMapper;

    private final ProductoCanalRepository productoCanalRepository;
    private final CalculoPrecioService calculoPrecioService;

    // =======================================
    // CRUD + DTOs
    // =======================================
    @Override
    @Transactional(readOnly = true)
    public List<CanalDTO> listar() {
        return canalRepository.findAll()
                .stream()
                .map(canalMapper::toDTO)
                .toList();
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
    // 🔥 LÓGICA DE NEGOCIO: CAMBIO DE MARGEN DEL CANAL
    // ===================================================
    @Override
    @Transactional
    public void actualizarMargen(Integer idCanal, BigDecimal nuevoMargen) {

        // 0) Validar canal
        Canal canal = canalRepository.findById(idCanal)
                .orElseThrow(() -> new NotFoundException("Canal no encontrado"));

        // 1) Obtener todos los productos asignados al canal
        List<ProductoCanal> productosDelCanal = productoCanalRepository.findByCanalId(idCanal);

        // 2) Actualizar margen (dirty checking hace el save)
        productosDelCanal.forEach(pc -> pc.setMargenPorcentaje(nuevoMargen));

        // 3) Recalcular precios
        productosDelCanal.forEach(pc ->
                calculoPrecioService.recalcularYGuardarPrecioCanal(
                        pc.getProducto().getId(),
                        idCanal
                )
        );
    }

}
