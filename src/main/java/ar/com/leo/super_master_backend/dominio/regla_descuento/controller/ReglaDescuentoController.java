package ar.com.leo.super_master_backend.dominio.regla_descuento.controller;

import ar.com.leo.super_master_backend.dominio.regla_descuento.dto.ReglaDescuentoCreateDTO;
import ar.com.leo.super_master_backend.dominio.regla_descuento.dto.ReglaDescuentoDTO;
import ar.com.leo.super_master_backend.dominio.regla_descuento.dto.ReglaDescuentoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.regla_descuento.service.ReglaDescuentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reglas-descuento")
public class ReglaDescuentoController {

    private final ReglaDescuentoService service;

    @GetMapping
    public ResponseEntity<List<ReglaDescuentoDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/canal/{canalId}")
    public ResponseEntity<List<ReglaDescuentoDTO>> listarPorCanal(@PathVariable Integer canalId) {
        return ResponseEntity.ok(service.listarPorCanal(canalId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReglaDescuentoDTO> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @PostMapping
    public ResponseEntity<ReglaDescuentoDTO> crear(@RequestBody ReglaDescuentoCreateDTO dto) {
        return ResponseEntity.ok(service.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReglaDescuentoDTO> actualizar(
            @PathVariable Integer id,
            @RequestBody ReglaDescuentoUpdateDTO dto
    ) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

}