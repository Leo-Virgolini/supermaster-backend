package ar.com.leo.super_master_backend.dominio.producto.controller;

import ar.com.leo.super_master_backend.dominio.producto.dto.EstadisticasDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.EstadisticasDTO.MargenesPorCuotasDTO;
import ar.com.leo.super_master_backend.dominio.producto.service.EstadisticasService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/estadisticas")
public class EstadisticasController {

    private final EstadisticasService service;

    @GetMapping
    @PreAuthorize("hasAuthority('PRODUCTOS_VER')")
    public ResponseEntity<EstadisticasDTO> obtenerEstadisticas() {
        return ResponseEntity.ok(service.obtenerEstadisticas());
    }

    @GetMapping("/margenes")
    @PreAuthorize("hasAuthority('PRODUCTOS_VER')")
    public ResponseEntity<MargenesPorCuotasDTO> obtenerMargenes(
            @RequestParam(required = false) Integer cuotas) {
        return ResponseEntity.ok(service.obtenerMargenesPorCuotas(cuotas));
    }
}
