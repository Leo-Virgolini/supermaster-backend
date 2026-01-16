package ar.com.leo.super_master_backend.dominio.producto.calculo.service;

import ar.com.leo.super_master_backend.dominio.canal.entity.*;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalConceptoCuotaRepository;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalConceptoReglaRepository;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalConceptoRepository;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalRepository;
import ar.com.leo.super_master_backend.dominio.common.exception.BadRequestException;
import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.entity.AplicaSobre;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.entity.ConceptoGasto;
import ar.com.leo.super_master_backend.dominio.producto.calculo.dto.FormulaCalculoDTO;
import ar.com.leo.super_master_backend.dominio.producto.calculo.dto.PrecioCalculadoDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.CanalPreciosDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.PrecioDTO;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoMargen;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanalPrecio;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanalPromocion;
import ar.com.leo.super_master_backend.dominio.producto.mla.repository.MlaRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.*;
import ar.com.leo.super_master_backend.dominio.promocion.entity.Promocion;
import ar.com.leo.super_master_backend.dominio.promocion.entity.TipoPromocionTabla;
import ar.com.leo.super_master_backend.dominio.regla_descuento.entity.ReglaDescuento;
import ar.com.leo.super_master_backend.dominio.regla_descuento.repository.ReglaDescuentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalculoPrecioServiceImpl implements CalculoPrecioService {

    private final ProductoRepository productoRepository;
    private final ProductoMargenRepository productoMargenRepository;
    private final ProductoCanalPrecioRepository productoCanalPrecioRepository;
    private final ProductoCanalPromocionRepository productoCanalPromocionRepository;
    private final ReglaDescuentoRepository reglaDescuentoRepository;
    private final ProductoCatalogoRepository productoCatalogoRepository;
    private final MlaRepository mlaRepository;
    private final CanalRepository canalRepository;
    private final CanalConceptoRepository canalConceptoRepository;
    private final CanalConceptoReglaRepository canalConceptoReglaRepository;
    private final CanalConceptoCuotaRepository canalConceptoCuotaRepository;

    // ====================================================
    // CONSTANTES
    // ====================================================
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final int PRECISION_CALCULO = 6;
    private static final int PRECISION_RESULTADO = 2;

    // ====================================================
    // API PÚBLICA
    // ====================================================
    @Override
    @Transactional(readOnly = true)
    public PrecioCalculadoDTO calcularPrecioCanal(Integer idProducto, Integer idCanal, Integer numeroCuotas) {
        Producto producto = obtenerProducto(idProducto);

        // Obtener todos los conceptos que aplican al canal según los filtros
        List<ConceptoGasto> conceptos = obtenerConceptosAplicables(idCanal, numeroCuotas, producto);
        List<CanalConcepto> conceptosCanal = convertirConceptosACanalConcepto(conceptos, idCanal);

        // Si el canal tiene canalBase o usa SOBRE_PVP_BASE, no requiere ProductoCanal
        Canal canal = canalRepository.findById(idCanal).orElse(null);
        boolean tieneCanalBase = canal != null && canal.getCanalBase() != null;
        boolean usaSobrePvpBase = tieneCanalBase || conceptos.stream()
                .anyMatch(c -> c.getAplicaSobre() == AplicaSobre.SOBRE_PVP_BASE);

        ProductoMargen productoMargen = usaSobrePvpBase
                ? null
                : obtenerProductoMargen(idProducto);

        return calcularPrecioUnificado(producto, productoMargen, conceptosCanal, numeroCuotas, idCanal);
    }

    @Override
    @Transactional
    public PrecioCalculadoDTO recalcularYGuardarPrecioCanal(Integer idProducto, Integer idCanal, Integer numeroCuotas) {
        Producto producto = obtenerProducto(idProducto);

        // Obtener todos los conceptos que aplican al canal según los filtros
        List<ConceptoGasto> conceptos = obtenerConceptosAplicables(idCanal, numeroCuotas, producto);
        List<CanalConcepto> conceptosCanal = convertirConceptosACanalConcepto(conceptos, idCanal);

        // Si el canal tiene canalBase o usa SOBRE_PVP_BASE, no requiere ProductoCanal
        Canal canal = canalRepository.findById(idCanal).orElse(null);
        boolean tieneCanalBase = canal != null && canal.getCanalBase() != null;
        boolean usaSobrePvpBase = tieneCanalBase || conceptos.stream()
                .anyMatch(c -> c.getAplicaSobre() == AplicaSobre.SOBRE_PVP_BASE);

        ProductoMargen productoMargen = usaSobrePvpBase
                ? null
                : obtenerProductoMargen(idProducto);

        PrecioCalculadoDTO dto = calcularPrecioUnificado(producto, productoMargen, conceptosCanal, numeroCuotas,
                idCanal);

        // Persistimos/actualizamos en producto_canal_precios (por producto, canal y cuotas)
        ProductoCanalPrecio pcp = productoCanalPrecioRepository
                .findByProductoIdAndCanalIdAndCuotas(idProducto, idCanal, numeroCuotas)
                .orElseGet(ProductoCanalPrecio::new);

        if (pcp.getId() == null) {
            pcp.setProducto(producto);
            Canal canalEntity = canal != null ? canal : canalRepository.findById(idCanal)
                    .orElseThrow(() -> new NotFoundException("Canal no encontrado"));
            pcp.setCanal(canalEntity);
            pcp.setCuotas(numeroCuotas);
        }

        pcp.setPvp(dto.pvp());
        pcp.setPvpInflado(dto.pvpInflado());
        pcp.setCostoTotal(dto.costoTotal());
        pcp.setGananciaAbs(dto.gananciaAbs());
        pcp.setGananciaPorcentaje(dto.gananciaPorcentaje());
        pcp.setMarkupPorcentaje(dto.markupPorcentaje());
        pcp.setFechaUltimoCalculo(LocalDateTime.now());

        productoCanalPrecioRepository.save(pcp);

        return dto;
    }

    // ====================================================
    // LÓGICA DE CÁLCULO
    // ====================================================

    /**
     * Calcula el precio de un producto para cualquier canal usando una fórmula unificada.
     * Esta fórmula reemplaza los métodos específicos por canal y elimina todo hardcodeo,
     * manejando todas las diferencias entre canales dinámicamente a través de conceptos y reglas.
     * <p>
     * Fórmula unificada:
     * 1. COSTO: Costo base del producto
     * 2. Gastos sobre COSTO: Conceptos con aplica_sobre='COSTO' (multiplican el costo)
     * 2.5. PROVEEDOR_FIN: Obtiene porcentaje de proveedores.porcentaje y aplica COSTO * (1 + %FIN/100)
     * 3. Ganancia: (GAN.MIN.ML + AUMENTA_MARGEN_PTS - REDUCE_MARGEN_PTS) * (1 + AUMENTA_MARGEN_PROP/100) * (1 - REDUCE_MARGEN_PROP/100)
     * 4. Costo con ganancia: COSTO_CON_GASTOS * (1 + GANANCIA/100)
     * 4.5. COSTO_GANANCIA: Conceptos con aplica_sobre='COSTO_GANANCIA' (multiplican después de ganancia, antes de IMP)
     * 5. Envío: Si existe concepto con aplica_sobre='ENVIO', buscar mlas.precio_envio y sumarlo
     * 6. Margen fijo: producto_canal.margen_fijo (suma absoluta)
     * 7. Impuestos (IMP): 1 + IVA/100 + sum(conceptos IMP)/100
     * 8. Gastos sobre COSTO_IVA: Conceptos con aplica_sobre='COSTO_IVA' (multiplican después de IMP)
     * 9. Gastos sobre PVP: Conceptos con aplica_sobre='PVP' + %CUOTAS (si aplica) - se aplican como divisores
     * 10. RECARGO_CUPON/DESCUENTO: Conceptos con aplica_sobre='RECARGO_CUPON' (divisor que aumenta precio) o 'DESCUENTO' (multiplicador que reduce precio)
     * 11. INFLACION: Conceptos con aplica_sobre='INFLACION' (divisor: PVP / (1 - INFLACION/100))
     * 12. Promociones: Promociones de producto_canal_promocion
     *
     * @param producto      El producto para calcular el precio
     * @param productoMargen La relación producto-canal con configuración específica
     * @param conceptos     Lista de conceptos ya filtrados por reglas según el producto y canal
     * @param numeroCuotas  Número de cuotas (null si pago contado)
     * @param idCanal       ID del canal para obtener información específica del canal
     * @return DTO con el precio calculado y métricas relacionadas
     */
    private PrecioCalculadoDTO calcularPrecioUnificado(
            Producto producto,
            ProductoMargen productoMargen,
            List<CanalConcepto> conceptos,
            Integer numeroCuotas,
            Integer idCanal) {
        if (producto.getCosto() == null || producto.getCosto().compareTo(BigDecimal.ZERO) == 0) {
            throw new BadRequestException("El producto no tiene costo cargado");
        }

        if (producto.getIva() == null) {
            throw new BadRequestException("El producto no tiene IVA cargado");
        }


        // ============================================
        // CASO ESPECIAL: CANAL CON CANAL_BASE
        // Si el canal tiene canalBase configurado, el cálculo se basa en el PVP del canal padre
        // ============================================
        Canal canalActual = canalRepository.findById(idCanal).orElse(null);
        boolean tieneCanalBase = canalActual != null && canalActual.getCanalBase() != null;

        if (tieneCanalBase) {
            // Pasar todos los conceptos para verificar PROVEEDOR_FIN y ENVIO
            return calcularPrecioSobrePvpBase(producto, productoMargen, conceptos, idCanal, numeroCuotas);
        }

        BigDecimal costo = producto.getCosto();
        BigDecimal iva = producto.getIva();

        // ============================================
        // PASO 1: COSTO BASE
        // ============================================
        BigDecimal costoBase = costo;

        // ============================================
        // PASO 2: Gastos sobre COSTO
        // ============================================
        List<CanalConcepto> gastosSobreCosto = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.COSTO)
                .collect(Collectors.toList());
        BigDecimal gastosSobreCostoTotal = calcularGastosPorcentaje(gastosSobreCosto);
        BigDecimal costoConGastos = costoBase.multiply(
                BigDecimal.ONE.add(gastosSobreCostoTotal.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP)));

        // ============================================
        // PASO 2.5: PROVEEDOR_FIN (financiación del proveedor)
        // ============================================
        List<CanalConcepto> conceptosProveedorFin = conceptos.stream()
                .filter(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.PROVEEDOR_FIN)
                .collect(Collectors.toList());

        BigDecimal porcentajeFin = BigDecimal.ZERO;
        if (!conceptosProveedorFin.isEmpty()) {
            // Obtener porcentaje de financiación del proveedor
            porcentajeFin = producto.getProveedor() != null
                    && producto.getProveedor().getPorcentaje() != null
                    ? producto.getProveedor().getPorcentaje()
                    : BigDecimal.ZERO;

            if (porcentajeFin.compareTo(BigDecimal.ZERO) > 0) {
                // Aplicar: COSTO * (1 + %FIN/100)
                costoConGastos = costoConGastos.multiply(
                        BigDecimal.ONE.add(porcentajeFin.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP)));
            }
        }

        // ============================================
        // PASO 3: Calcular ganancia ajustada
        // ============================================
        BigDecimal margenPorcentaje = obtenerMargenPorcentaje(productoMargen, conceptos);

        // Obtener AUMENTA_MARGEN
        List<CanalConcepto> conceptosAumentaMargen = conceptos.stream()
                .filter(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.AUMENTA_MARGEN_PTS)
                .collect(Collectors.toList());
        BigDecimal aumentaMargen = calcularGastosPorcentaje(conceptosAumentaMargen);

        // Obtener REDUCE_MARGEN
        List<CanalConcepto> conceptosReduceMargen = conceptos.stream()
                .filter(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.REDUCE_MARGEN_PTS)
                .collect(Collectors.toList());
        BigDecimal reduceMargen = calcularGastosPorcentaje(conceptosReduceMargen);

        // Obtener AUMENTA_MARGEN_PROP (proporcional)
        List<CanalConcepto> conceptosAumentaMargenProp = conceptos.stream()
                .filter(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.AUMENTA_MARGEN_PROP)
                .collect(Collectors.toList());
        BigDecimal aumentaMargenProp = calcularGastosPorcentaje(conceptosAumentaMargenProp);

        // Obtener REDUCE_MARGEN_PROP (proporcional)
        List<CanalConcepto> conceptosReduceMargenProp = conceptos.stream()
                .filter(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.REDUCE_MARGEN_PROP)
                .collect(Collectors.toList());
        BigDecimal reduceMargenProp = calcularGastosPorcentaje(conceptosReduceMargenProp);

        // Calcular ganancia ajustada: GAN.MIN.ML + AUMENTA_MARGEN_PTS - REDUCE_MARGEN_PTS
        BigDecimal gananciaAjustada = margenPorcentaje;
        if (aumentaMargen.compareTo(BigDecimal.ZERO) > 0) {
            gananciaAjustada = gananciaAjustada.add(aumentaMargen);
        }
        if (reduceMargen.compareTo(BigDecimal.ZERO) > 0) {
            gananciaAjustada = gananciaAjustada.subtract(reduceMargen);
        }

        // Aplicar ajustes proporcionales: ganancia * (1 +/- porcentaje/100)
        if (aumentaMargenProp.compareTo(BigDecimal.ZERO) > 0) {
            // ganancia * (1 + porcentaje/100)
            gananciaAjustada = gananciaAjustada.multiply(
                    BigDecimal.ONE.add(aumentaMargenProp.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP)));
        }
        if (reduceMargenProp.compareTo(BigDecimal.ZERO) > 0) {
            // ganancia * (1 - porcentaje/100)
            gananciaAjustada = gananciaAjustada.multiply(
                    BigDecimal.ONE.subtract(reduceMargenProp.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP)));
        }

        // Si GAN.MIN.ML = 0 y no hay ajustes, usar 0
        BigDecimal gananciaUsar = gananciaAjustada;

        // ============================================
        // PASO 4: Calcular costo con ganancia
        // ============================================
        BigDecimal gananciaFrac = gananciaUsar.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP);
        BigDecimal costoConGanancia = costoConGastos.multiply(BigDecimal.ONE.add(gananciaFrac));

        // ============================================
        // PASO 4.5: COSTO_GANANCIA (conceptos después de ganancia, antes de IMP)
        // ============================================
        List<CanalConcepto> gastosSobreCostoGanancia = conceptos.stream()
                .filter(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.COSTO_GANANCIA)
                .collect(Collectors.toList());
        BigDecimal gastosSobreCostoGananciaTotal = calcularGastosPorcentaje(gastosSobreCostoGanancia);
        if (gastosSobreCostoGananciaTotal.compareTo(BigDecimal.ZERO) > 0) {
            // Aplicar: COSTO_CON_GANANCIA * (1 + concepto/100)
            costoConGanancia = costoConGanancia.multiply(
                    BigDecimal.ONE.add(gastosSobreCostoGananciaTotal.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP)));
        }

        // ============================================
        // PASO 5: Envío (si existe concepto ENVIO)
        // ============================================
        BigDecimal costoConGananciaYEnvio = costoConGanancia;
        BigDecimal precioEnvio = BigDecimal.ZERO;
        List<CanalConcepto> conceptosEnvio = conceptos.stream()
                .filter(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.ENVIO)
                .collect(Collectors.toList());

        if (!conceptosEnvio.isEmpty()) {
            // Buscar precio_envio del MLA asociado al producto
            if (producto.getMla() != null && producto.getMla().getPrecioEnvio() != null
                    && producto.getMla().getPrecioEnvio().compareTo(BigDecimal.ZERO) > 0) {
                precioEnvio = producto.getMla().getPrecioEnvio();
            }

            if (precioEnvio.compareTo(BigDecimal.ZERO) > 0) {
                costoConGananciaYEnvio = costoConGanancia.add(precioEnvio);
            }
        }

        // ============================================
        // PASO 6: Margen fijo (de producto_canal)
        // ============================================
        BigDecimal margenFijo = obtenerMargenFijo(productoMargen, conceptos);
        // Nota: El margen fijo se aplicará al final sobre el PVP, no aquí

        // ============================================
        // PASO 7: Calcular factor de impuestos (IMP)
        // ============================================
        // El IVA del producto solo se aplica si existe un concepto con aplicaSobre=IVA para el canal
        boolean aplicaIva = conceptos.stream()
                .anyMatch(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.IVA);
        BigDecimal ivaAplicar = aplicaIva ? iva : BigDecimal.ZERO;

        // Las promociones solo se aplican si existe un concepto con aplicaSobre=PROMOCION para el canal
        boolean usaPromociones = conceptos.stream()
                .anyMatch(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.PROMOCION);

        List<CanalConcepto> gastosSobreImp = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.IMP)
                .collect(Collectors.toList());
        BigDecimal gastosSobreImpTotal = calcularGastosPorcentaje(gastosSobreImp);
        BigDecimal imp = BigDecimal.ONE
                .add(ivaAplicar.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP))
                .add(gastosSobreImpTotal.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP));

        // ============================================
        // PASO 8: Aplicar impuestos
        // ============================================
        BigDecimal costoConImpuestos = costoConGananciaYEnvio.multiply(imp);

        // ============================================
        // PASO 9: Gastos sobre COSTO_IVA
        // ============================================
        List<CanalConcepto> gastosSobreCostoIva = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.COSTO_IVA)
                .collect(Collectors.toList());
        BigDecimal gastosSobreCostoIvaTotal = calcularGastosPorcentaje(gastosSobreCostoIva);
        if (gastosSobreCostoIvaTotal.compareTo(BigDecimal.ZERO) > 0) {
            costoConImpuestos = costoConImpuestos.multiply(
                    BigDecimal.ONE.add(gastosSobreCostoIvaTotal.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP)));
        }

        // ============================================
        // PASO 10: Gastos sobre PVP y cuotas
        // ============================================
        // Lógica especial: Si hay cuotas y aplicaCuotas=true, los gastos PVP se incluyen en el cálculo de cuotas
        // Si no hay cuotas, se aplican directamente como divisor
        BigDecimal gastosSobrePVPTotal = BigDecimal.ZERO;
        BigDecimal porcentajeCuota = BigDecimal.ZERO;

        // Obtener gastos PVP
        List<CanalConcepto> gastosSobrePVP = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.PVP)
                .collect(Collectors.toList());
        gastosSobrePVPTotal = calcularGastosPorcentaje(gastosSobrePVP);

        // Obtener porcentaje de cuotas si aplica (ahora siempre aplica si hay cuotas)
        boolean aplicarCuotas = numeroCuotas != null;

        if (aplicarCuotas) {
            porcentajeCuota = obtenerPorcentajeCuota(idCanal, numeroCuotas);

            // Usar gastos PVP de los conceptos ya filtrados (con reglas aplicadas)
            BigDecimal porcentajeConceptosCanal = gastosSobrePVPTotal;

            if (porcentajeCuota.compareTo(BigDecimal.ZERO) >= 0) {
                // CUOTA >= 0 (interés o sin cambio): sumar a gastos PVP y aplicar juntos como divisor
                // Fórmula: pvp = costo / (1 - (gastos_pvp% + cuota%)/100)
                BigDecimal porcentajeCuotasTotal = porcentajeConceptosCanal.add(porcentajeCuota);

                if (porcentajeCuotasTotal.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal cuotasFrac = porcentajeCuotasTotal.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP);
                    BigDecimal divisorCuotas = BigDecimal.ONE.subtract(cuotasFrac);
                    if (divisorCuotas.compareTo(BigDecimal.ZERO) > 0) {
                        costoConImpuestos = costoConImpuestos.divide(divisorCuotas, PRECISION_CALCULO, RoundingMode.HALF_UP);
                    }
                }
            } else {
                // CUOTA < 0 (descuento): aplicar gastos PVP primero, luego descuento como multiplicador
                // Fórmula: pvp = (costo / (1 - gastos_pvp%/100)) * (1 - |descuento|%/100)

                // Primero gastos PVP
                if (porcentajeConceptosCanal.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal gastosFrac = porcentajeConceptosCanal.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP);
                    BigDecimal divisorGastos = BigDecimal.ONE.subtract(gastosFrac);
                    if (divisorGastos.compareTo(BigDecimal.ZERO) > 0) {
                        costoConImpuestos = costoConImpuestos.divide(divisorGastos, PRECISION_CALCULO, RoundingMode.HALF_UP);
                    }
                }

                // Luego descuento como multiplicador
                BigDecimal descuentoFrac = porcentajeCuota.abs().divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP);
                BigDecimal factorDescuento = BigDecimal.ONE.subtract(descuentoFrac);
                costoConImpuestos = costoConImpuestos.multiply(factorDescuento);
            }

            // En este caso, no aplicamos gastos PVP como divisor separado (ya se aplicaron arriba)
            gastosSobrePVPTotal = BigDecimal.ZERO;
        } else {
            // Si no hay cuotas, aplicar gastos PVP como divisor
            if (gastosSobrePVPTotal.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal gastosSobrePVPFrac = gastosSobrePVPTotal.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP);
                BigDecimal denominador = BigDecimal.ONE.subtract(gastosSobrePVPFrac);
                if (denominador.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BadRequestException("Los gastos sobre PVP son >= 100%, lo cual es inválido");
                }
                costoConImpuestos = costoConImpuestos.divide(denominador, PRECISION_CALCULO, RoundingMode.HALF_UP);
            }
        }

        BigDecimal pvpBase = costoConImpuestos;

        // ============================================
        // PASO 11: RECARGO_CUPON y DESCUENTO (conceptos)
        // ============================================
        // Obtener RECARGO_CUPON
        List<CanalConcepto> conceptosRecargoCupon = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.RECARGO_CUPON)
                .collect(Collectors.toList());
        BigDecimal porcentajeRecargoCupon = calcularGastosPorcentaje(conceptosRecargoCupon);

        // Obtener DESCUENTO (conceptos con aplica_sobre='DESCUENTO')
        BigDecimal descuentoConceptos = obtenerDescuentoMaquina(conceptos);

        BigDecimal pvp = pvpBase;

        // Aplicar RECARGO_CUPON como divisor: / (1 - RECARGO_CUPON/100) - aumenta el precio
        if (porcentajeRecargoCupon.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal recargoCuponFrac = porcentajeRecargoCupon.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP);
            BigDecimal denominadorRecargoCupon = BigDecimal.ONE.subtract(recargoCuponFrac);
            if (denominadorRecargoCupon.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("RECARGO_CUPON es >= 100%, lo cual es inválido");
            }
            pvp = pvp.divide(denominadorRecargoCupon, PRECISION_CALCULO, RoundingMode.HALF_UP);
        }

        // Aplicar DESCUENTO como multiplicador: * (1 - DESCUENTO/100)
        if (descuentoConceptos.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal descuentoFrac = descuentoConceptos.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP);
            BigDecimal factorDescuento = BigDecimal.ONE.subtract(descuentoFrac);
            pvp = pvp.multiply(factorDescuento);
        }

        // ============================================
        // PASO 12: Aplicar reglas de descuento (si existen)
        // ============================================
        BigDecimal descuentoTotal = obtenerDescuentoAplicable(producto, idCanal, pvp);
        if (descuentoTotal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal descuentoFrac = descuentoTotal.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP);
            BigDecimal denominadorDescuento = BigDecimal.ONE.subtract(descuentoFrac);
            if (denominadorDescuento.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("El descuento es >= 100%, lo cual es inválido");
            }
            pvp = pvp.divide(denominadorDescuento, PRECISION_CALCULO, RoundingMode.HALF_UP);
        }

        // ============================================
        // PASO 13: Margen fijo
        // ============================================
        if (margenFijo.compareTo(BigDecimal.ZERO) > 0) {
            pvp = pvp.add(margenFijo);
        }

        // ============================================
        // PASO 14: INFLACION (concepto)
        // ============================================
        List<CanalConcepto> conceptosInflacion = conceptos.stream()
                .filter(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.INFLACION)
                .collect(Collectors.toList());

        if (!conceptosInflacion.isEmpty()) {
            BigDecimal porcentajeInflacion = calcularGastosPorcentaje(conceptosInflacion);
            if (porcentajeInflacion.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal divisor = BigDecimal.ONE.subtract(porcentajeInflacion.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP));
                if (divisor.compareTo(BigDecimal.ZERO) > 0) {
                    pvp = pvp.divide(divisor, PRECISION_CALCULO, RoundingMode.HALF_UP);
                }
            }
        }

        BigDecimal pvpSinPromocion = pvp.setScale(PRECISION_RESULTADO, RoundingMode.HALF_UP);

        // ============================================
        // PASO 15: Promociones (solo si el canal tiene concepto PROMOCION habilitado)
        // ============================================
        BigDecimal pvpInflado = aplicarPromocionSinInflacion(producto.getId(), idCanal,
                pvpSinPromocion, usaPromociones);
        pvpInflado = pvpInflado.setScale(PRECISION_RESULTADO, RoundingMode.HALF_UP);

        // ============================================
        // PASO 16: Calcular métricas
        // ============================================
        // costoTotal = costoBase + financiación proveedor + envío
        BigDecimal costoTotal = costoBase
                .multiply(BigDecimal.ONE.add(porcentajeFin.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP)))
                .add(precioEnvio)
                .setScale(PRECISION_RESULTADO, RoundingMode.HALF_UP);

        // Calcular ganancia absoluta: pvp - costoTotal
        BigDecimal gananciaAbs = pvpSinPromocion.subtract(costoTotal)
                .setScale(PRECISION_RESULTADO, RoundingMode.HALF_UP);

        // Calcular gananciaPorcentaje: (gananciaAbs / pvp) * 100
        BigDecimal gananciaPorcentaje = BigDecimal.ZERO;
        if (pvpSinPromocion.compareTo(BigDecimal.ZERO) > 0) {
            gananciaPorcentaje = gananciaAbs
                    .multiply(CIEN)
                    .divide(pvpSinPromocion, PRECISION_RESULTADO, RoundingMode.HALF_UP);
        }

        // Calcular markupPorcentaje: (gananciaAbs / costoTotal) * 100
        BigDecimal markupPorcentaje = BigDecimal.ZERO;
        if (costoTotal.compareTo(BigDecimal.ZERO) > 0) {
            markupPorcentaje = gananciaAbs
                    .multiply(CIEN)
                    .divide(costoTotal, PRECISION_RESULTADO, RoundingMode.HALF_UP);
        }

        return new PrecioCalculadoDTO(
                idCanal,
                canalActual != null ? canalActual.getCanal() : null,
                numeroCuotas,
                pvpSinPromocion,
                pvpInflado,
                costoTotal,
                gananciaAbs,
                gananciaPorcentaje,
                markupPorcentaje,
                LocalDateTime.now());
    }

    /**
     * Genera la fórmula de cálculo de precio para cualquier canal usando una fórmula unificada.
     * Esta fórmula reemplaza los métodos específicos por canal y muestra todos los pasos dinámicamente.
     *
     * @param producto      El producto para calcular la fórmula
     * @param productoMargen La relación producto-canal con configuración específica
     * @param conceptos     Lista de conceptos ya filtrados por reglas según el producto y canal
     * @param numeroCuotas  Número de cuotas (null si pago contado)
     * @param idCanal       ID del canal para obtener información específica del canal
     * @return DTO con los pasos de la fórmula y el resultado final
     */
    private FormulaCalculoDTO generarFormulaUnificado(
            Producto producto,
            ProductoMargen productoMargen,
            List<CanalConcepto> conceptos,
            Integer numeroCuotas,
            Integer idCanal) {
        List<FormulaCalculoDTO.PasoCalculo> pasos = new java.util.ArrayList<>();
        int pasoNumero = 1;

        // ============================================
        // CASO ESPECIAL: CANAL CON CANAL_BASE
        // Si el canal tiene canalBase configurado, la fórmula se basa en el PVP del canal padre
        // ============================================
        Canal canalActual = canalRepository.findById(idCanal).orElse(null);
        boolean tieneCanalBase = canalActual != null && canalActual.getCanalBase() != null;

        if (tieneCanalBase) {
            List<CanalConcepto> conceptosSobrePvpBase = conceptos.stream()
                    .filter(cc -> cc.getConcepto() != null
                            && cc.getConcepto().getAplicaSobre() == AplicaSobre.SOBRE_PVP_BASE)
                    .collect(Collectors.toList());
            return generarFormulaSobrePvpBase(producto, conceptosSobrePvpBase, idCanal);
        }

        // Paso 1: COSTO BASE
        BigDecimal costo = producto.getCosto();
        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++, "Costo base del producto",
                "COSTO", rd(costo), String.format("Costo: $%s", fmt(costo))));

        // Paso 2: Gastos sobre COSTO
        List<CanalConcepto> gastosSobreCosto = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.COSTO)
                .collect(Collectors.toList());
        BigDecimal gastosSobreCostoTotal = calcularGastosPorcentaje(gastosSobreCosto);
        BigDecimal costoConGastos = costo;
        if (gastosSobreCostoTotal.compareTo(BigDecimal.ZERO) > 0) {
            costoConGastos = costo.multiply(
                    BigDecimal.ONE.add(gastosSobreCostoTotal.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP)));
            List<String> nombresConceptosCosto = obtenerNombresConceptos(conceptos, AplicaSobre.COSTO);
            String nombresCostoFormateados = formatearNombresConceptos(nombresConceptosCosto);
            String detalleConceptosCosto = formatearDetalleConceptos(gastosSobreCosto);
            pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                    "Gastos sobre COSTO",
                    String.format("COSTO_CON_GASTOS = COSTO * (1 + (%s)/100)", nombresCostoFormateados),
                    rd(costoConGastos),
                    String.format("%s * (1 + (%s)/100) = %s", fmt(costo),
                            detalleConceptosCosto.isEmpty() ? fmt(gastosSobreCostoTotal) : detalleConceptosCosto,
                            fmt(costoConGastos))));
        }

        // Paso 2.5: PROVEEDOR_FIN (financiación del proveedor)
        List<CanalConcepto> conceptosProveedorFin = conceptos.stream()
                .filter(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.PROVEEDOR_FIN)
                .collect(Collectors.toList());

        if (!conceptosProveedorFin.isEmpty()) {
            BigDecimal porcentajeFin = producto.getProveedor() != null
                    && producto.getProveedor().getPorcentaje() != null
                    ? producto.getProveedor().getPorcentaje()
                    : BigDecimal.ZERO;

            if (porcentajeFin.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal costoAntesFin = costoConGastos;
                costoConGastos = costoConGastos.multiply(
                        BigDecimal.ONE.add(porcentajeFin.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP)));
                List<String> nombresProveedorFin = obtenerNombresConceptos(conceptos, AplicaSobre.PROVEEDOR_FIN);
                String nombresProveedorFinFormateados = formatearNombresConceptos(nombresProveedorFin);
                pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                        "Financiación del proveedor",
                        String.format("COSTO_CON_FINANCIACION = COSTO_CON_GASTOS * (1 + %%FIN/100)",
                                nombresProveedorFinFormateados),
                        rd(costoConGastos),
                        String.format("%s * (1 + %s/100) = %s (%%FIN obtenido de proveedor: %s%%)",
                                fmt(costoAntesFin),
                                fmt(porcentajeFin),
                                fmt(costoConGastos),
                                fmt(porcentajeFin))));
            }
        }

        // Paso 3: Ganancia ajustada
        BigDecimal margenPorcentaje = obtenerMargenPorcentaje(productoMargen, conceptos);

        List<CanalConcepto> conceptosAumentaMargen = conceptos.stream()
                .filter(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.AUMENTA_MARGEN_PTS)
                .collect(Collectors.toList());
        BigDecimal aumentaMargen = calcularGastosPorcentaje(conceptosAumentaMargen);

        List<CanalConcepto> conceptosReduceMargen = conceptos.stream()
                .filter(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.REDUCE_MARGEN_PTS)
                .collect(Collectors.toList());
        BigDecimal reduceMargen = calcularGastosPorcentaje(conceptosReduceMargen);

        // Obtener AUMENTA_MARGEN_PROP (proporcional)
        List<CanalConcepto> conceptosAumentaMargenProp = conceptos.stream()
                .filter(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.AUMENTA_MARGEN_PROP)
                .collect(Collectors.toList());
        BigDecimal aumentaMargenProp = calcularGastosPorcentaje(conceptosAumentaMargenProp);

        // Obtener REDUCE_MARGEN_PROP (proporcional)
        List<CanalConcepto> conceptosReduceMargenProp = conceptos.stream()
                .filter(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.REDUCE_MARGEN_PROP)
                .collect(Collectors.toList());
        BigDecimal reduceMargenProp = calcularGastosPorcentaje(conceptosReduceMargenProp);

        BigDecimal gananciaAjustada = margenPorcentaje;
        if (aumentaMargen.compareTo(BigDecimal.ZERO) > 0) {
            gananciaAjustada = gananciaAjustada.add(aumentaMargen);
        }
        if (reduceMargen.compareTo(BigDecimal.ZERO) > 0) {
            gananciaAjustada = gananciaAjustada.subtract(reduceMargen);
        }

        // Aplicar ajustes proporcionales: ganancia * (1 +/- porcentaje/100)
        if (aumentaMargenProp.compareTo(BigDecimal.ZERO) > 0) {
            gananciaAjustada = gananciaAjustada.multiply(
                    BigDecimal.ONE.add(aumentaMargenProp.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP)));
        }
        if (reduceMargenProp.compareTo(BigDecimal.ZERO) > 0) {
            gananciaAjustada = gananciaAjustada.multiply(
                    BigDecimal.ONE.subtract(reduceMargenProp.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP)));
        }

        BigDecimal gananciaUsar = gananciaAjustada;

        List<String> nombresAumentaMargen = obtenerNombresConceptos(conceptos, AplicaSobre.AUMENTA_MARGEN_PTS);
        List<String> nombresReduceMargen = obtenerNombresConceptos(conceptos, AplicaSobre.REDUCE_MARGEN_PTS);
        List<String> nombresAumentaMargenProp = obtenerNombresConceptos(conceptos, AplicaSobre.AUMENTA_MARGEN_PROP);
        List<String> nombresReduceMargenProp = obtenerNombresConceptos(conceptos, AplicaSobre.REDUCE_MARGEN_PROP);
        String nombresAumentaMargenFormateados = formatearNombresConceptos(nombresAumentaMargen);
        String nombresReduceMargenFormateados = formatearNombresConceptos(nombresReduceMargen);
        String nombresAumentaMargenPropFormateados = formatearNombresConceptos(nombresAumentaMargenProp);
        String nombresReduceMargenPropFormateados = formatearNombresConceptos(nombresReduceMargenProp);
        String detalleAumentaMargen = formatearDetalleConceptos(conceptosAumentaMargen);
        String detalleReduceMargen = formatearDetalleConceptos(conceptosReduceMargen);
        String detalleAumentaMargenProp = formatearDetalleConceptos(conceptosAumentaMargenProp);
        String detalleReduceMargenProp = formatearDetalleConceptos(conceptosReduceMargenProp);

        String formulaGanancia = "GANANCIA = MARGEN";
        String detalleGanancia = String.format("MARGEN: %s%%", margenPorcentaje);
        if (aumentaMargen.compareTo(BigDecimal.ZERO) > 0) {
            formulaGanancia += String.format(" + %s", nombresAumentaMargenFormateados);
            detalleGanancia += String.format(" + %s", detalleAumentaMargen);
        }
        if (reduceMargen.compareTo(BigDecimal.ZERO) > 0) {
            formulaGanancia += String.format(" - %s", nombresReduceMargenFormateados);
            detalleGanancia += String.format(" - %s", detalleReduceMargen);
        }
        if (aumentaMargenProp.compareTo(BigDecimal.ZERO) > 0) {
            formulaGanancia += String.format(" * (1 + %s/100)", nombresAumentaMargenPropFormateados);
            detalleGanancia += String.format(" * (1 + %s/100)", detalleAumentaMargenProp);
        }
        if (reduceMargenProp.compareTo(BigDecimal.ZERO) > 0) {
            formulaGanancia += String.format(" * (1 - %s/100)", nombresReduceMargenPropFormateados);
            detalleGanancia += String.format(" * (1 - %s/100)", detalleReduceMargenProp);
        }
        detalleGanancia += String.format(" → GANANCIA = %s%%", fmt(gananciaUsar));

        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++, "Ganancia ajustada",
                formulaGanancia, rd(gananciaUsar), detalleGanancia));

        // Paso 4: Costo con ganancia
        BigDecimal gananciaFrac = gananciaUsar.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP);
        BigDecimal costoConGanancia = costoConGastos.multiply(BigDecimal.ONE.add(gananciaFrac));
        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++, "Costo con ganancia",
                "COSTO_CON_GANANCIA = COSTO_CON_GASTOS * (1 + GANANCIA/100)",
                rd(costoConGanancia),
                String.format("%s * (1 + %s/100) = %s", fmt(costoConGastos), fmt(gananciaUsar), fmt(costoConGanancia))));

        // Paso 4.5: Gastos sobre COSTO_GANANCIA (después de ganancia, antes de IMP)
        List<CanalConcepto> gastosSobreCostoGanancia = conceptos.stream()
                .filter(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.COSTO_GANANCIA)
                .collect(Collectors.toList());
        BigDecimal gastosSobreCostoGananciaTotal = calcularGastosPorcentaje(gastosSobreCostoGanancia);
        if (gastosSobreCostoGananciaTotal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal costoAntesCostoGanancia = costoConGanancia;
            costoConGanancia = costoConGanancia.multiply(
                    BigDecimal.ONE.add(gastosSobreCostoGananciaTotal.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP)));
            List<String> nombresCostoGanancia = obtenerNombresConceptos(conceptos, AplicaSobre.COSTO_GANANCIA);
            String nombresCostoGananciaFormateados = formatearNombresConceptos(nombresCostoGanancia);
            String detalleCostoGanancia = formatearDetalleConceptos(gastosSobreCostoGanancia);
            pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                    "Gastos sobre COSTO_GANANCIA",
                    String.format("COSTO_CON_GANANCIA = COSTO_CON_GANANCIA * (1 + (%s)/100)",
                            nombresCostoGananciaFormateados),
                    rd(costoConGanancia),
                    String.format("%s * (1 + (%s)/100) = %s",
                            fmt(costoAntesCostoGanancia),
                            detalleCostoGanancia.isEmpty() ? fmt(gastosSobreCostoGananciaTotal) : detalleCostoGanancia,
                            fmt(costoConGanancia))));
        }

        // Paso 5: Envío (si existe concepto ENVIO)
        BigDecimal costoConGananciaYEnvio = costoConGanancia;
        List<CanalConcepto> conceptosEnvio = conceptos.stream()
                .filter(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.ENVIO)
                .collect(Collectors.toList());

        if (!conceptosEnvio.isEmpty()) {
            BigDecimal precioEnvio = BigDecimal.ZERO;
            if (producto.getMla() != null && producto.getMla().getPrecioEnvio() != null
                    && producto.getMla().getPrecioEnvio().compareTo(BigDecimal.ZERO) > 0) {
                precioEnvio = producto.getMla().getPrecioEnvio();
            }

            if (precioEnvio.compareTo(BigDecimal.ZERO) > 0) {
                costoConGananciaYEnvio = costoConGanancia.add(precioEnvio);
                pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++, "Sumar envío",
                        "COSTO_CON_GANANCIA_Y_ENVIO = COSTO_CON_GANANCIA + ENVIO",
                        rd(costoConGananciaYEnvio),
                        String.format("Envío: $%s → %s + %s = %s", fmt(precioEnvio), fmt(costoConGanancia), fmt(precioEnvio),
                                fmt(costoConGananciaYEnvio))));
            }
        }

        // Paso 6: Factor de impuestos (IMP)
        // El IVA del producto solo se aplica si existe un concepto con aplicaSobre=IVA para el canal
        BigDecimal ivaProducto = producto.getIva();
        boolean aplicaIva = conceptos.stream()
                .anyMatch(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.IVA);
        BigDecimal ivaAplicar = aplicaIva ? ivaProducto : BigDecimal.ZERO;

        // Las promociones solo se aplican si existe un concepto con aplicaSobre=PROMOCION para el canal
        boolean usaPromociones = conceptos.stream()
                .anyMatch(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.PROMOCION);

        List<CanalConcepto> gastosSobreImp = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.IMP)
                .collect(Collectors.toList());
        BigDecimal gastosSobreImpTotal = calcularGastosPorcentaje(gastosSobreImp);
        BigDecimal imp = BigDecimal.ONE
                .add(ivaAplicar.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP))
                .add(gastosSobreImpTotal.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP));

        List<String> nombresConceptosImp = obtenerNombresConceptos(conceptos, AplicaSobre.IMP);
        String nombresImpFormateados = formatearNombresConceptos(nombresConceptosImp);
        String detalleConceptosImp = formatearDetalleConceptos(gastosSobreImp);
        String formulaImp = nombresImpFormateados.isEmpty()
                ? "IMP = 1 + IVA/100"
                : String.format("IMP = 1 + IVA/100 + %s/100", nombresImpFormateados);

        String fuenteIva = aplicaIva
                ? String.format("IVA (producto): %s%%", fmt(ivaAplicar))
                : "IVA: 0% (canal sin concepto IVA)";
        String detalleImp = fuenteIva;
        if (gastosSobreImpTotal.compareTo(BigDecimal.ZERO) > 0 && !detalleConceptosImp.isEmpty()) {
            detalleImp += String.format(" + %s", detalleConceptosImp);
        }
        detalleImp += String.format(" → IMP = 1 + %s/100", fmt(ivaAplicar));
        if (gastosSobreImpTotal.compareTo(BigDecimal.ZERO) > 0) {
            detalleImp += String.format(" + %s/100 = %s", fmt(gastosSobreImpTotal), fmt(imp));
        } else {
            detalleImp += String.format(" = %s", fmt(imp));
        }
        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++, "Factor de impuestos (IMP)",
                formulaImp, rd(imp), detalleImp));

        // Paso 7: Aplicar impuestos
        BigDecimal costoConImpuestos = costoConGananciaYEnvio.multiply(imp);
        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++, "Costo con impuestos",
                "COSTO_CON_IMPUESTOS = COSTO_CON_GANANCIA_Y_ENVIO * IMP",
                rd(costoConImpuestos),
                String.format("%s * %s = %s", fmt(costoConGananciaYEnvio), fmt(imp), fmt(costoConImpuestos))));

        // Paso 8: Gastos sobre COSTO_IVA
        List<CanalConcepto> gastosSobreCostoIva = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.COSTO_IVA)
                .collect(Collectors.toList());
        BigDecimal gastosSobreCostoIvaTotal = calcularGastosPorcentaje(gastosSobreCostoIva);
        if (gastosSobreCostoIvaTotal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal costoConImpuestosAntes = costoConImpuestos;
            costoConImpuestos = costoConImpuestos.multiply(
                    BigDecimal.ONE.add(gastosSobreCostoIvaTotal.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP)));
            List<String> nombresConceptosCostoIva = obtenerNombresConceptos(conceptos, AplicaSobre.COSTO_IVA);
            String nombresCostoIvaFormateados = formatearNombresConceptos(nombresConceptosCostoIva);
            String detalleConceptosCostoIva = formatearDetalleConceptos(gastosSobreCostoIva);
            pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                    "Gastos sobre COSTO_IVA",
                    String.format("COSTO_CON_IMPUESTOS = COSTO_CON_IMPUESTOS * (1 + (%s)/100)",
                            nombresCostoIvaFormateados),
                    rd(costoConImpuestos),
                    String.format("%s * (1 + (%s)/100) = %s", fmt(costoConImpuestosAntes),
                            detalleConceptosCostoIva.isEmpty() ? fmt(gastosSobreCostoIvaTotal) : detalleConceptosCostoIva,
                            fmt(costoConImpuestos))));
        }

        // Paso 9: Gastos sobre PVP y cuotas
        List<CanalConcepto> gastosSobrePVP = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.PVP)
                .collect(Collectors.toList());
        BigDecimal gastosSobrePVPTotal = calcularGastosPorcentaje(gastosSobrePVP);
        BigDecimal porcentajeCuota = BigDecimal.ZERO;

        boolean aplicarCuotas = numeroCuotas != null;

        if (aplicarCuotas) {
            porcentajeCuota = obtenerPorcentajeCuota(idCanal, numeroCuotas);

            // Usar los conceptos ya filtrados (con reglas aplicadas) en lugar de buscar todos
            BigDecimal porcentajeConceptosCanal = gastosSobrePVPTotal;

            BigDecimal porcentajeCuotasTotal = porcentajeConceptosCanal.add(porcentajeCuota);

            if (porcentajeCuotasTotal.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal cuotasFrac = porcentajeCuotasTotal.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP);
                BigDecimal divisorCuotas = BigDecimal.ONE.subtract(cuotasFrac);
                if (divisorCuotas.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal costoConImpuestosAntesCuotas = costoConImpuestos;
                    costoConImpuestos = costoConImpuestos.divide(divisorCuotas, PRECISION_CALCULO, RoundingMode.HALF_UP);
                    List<String> nombresConceptosPVPCuotas = obtenerNombresConceptos(conceptos, AplicaSobre.PVP);
                    String nombresPVPCuotasFormateados = formatearNombresConceptos(nombresConceptosPVPCuotas);
                    String detalleConceptosPVPCuotas = formatearDetalleConceptos(gastosSobrePVP);
                    pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                            "Aplicar cuotas (GTML[%])",
                            String.format("PVP = COSTO_CON_IMPUESTOS / (1 - (%s + %s cuotas)/100)",
                                    nombresPVPCuotasFormateados, numeroCuotas),
                            rd(costoConImpuestos),
                            String.format("GTML[%%] = (%s) + %s%% = %s%% → %s / (1 - %s/100) = %s",
                                    detalleConceptosPVPCuotas.isEmpty() ? fmt(porcentajeConceptosCanal) + "%"
                                            : detalleConceptosPVPCuotas,
                                    fmt(porcentajeCuota), fmt(porcentajeCuotasTotal),
                                    fmt(costoConImpuestosAntesCuotas), fmt(porcentajeCuotasTotal), fmt(costoConImpuestos))));
                }
            }
            gastosSobrePVPTotal = BigDecimal.ZERO;
        } else {
            if (gastosSobrePVPTotal.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal gastosSobrePVPFrac = gastosSobrePVPTotal.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP);
                BigDecimal denominador = BigDecimal.ONE.subtract(gastosSobrePVPFrac);
                if (denominador.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BadRequestException("Los gastos sobre PVP son >= 100%, lo cual es inválido");
                }
                BigDecimal costoConImpuestosAntesPVP = costoConImpuestos;
                costoConImpuestos = costoConImpuestos.divide(denominador, PRECISION_CALCULO, RoundingMode.HALF_UP);
                List<String> nombresConceptosPVP = obtenerNombresConceptos(conceptos, AplicaSobre.PVP);
                String nombresPVPFormateados = formatearNombresConceptos(nombresConceptosPVP);
                String detalleConceptosPVP = formatearDetalleConceptos(gastosSobrePVP);
                pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                        "Gastos sobre PVP",
                        String.format("PVP = COSTO_CON_IMPUESTOS / (1 - (%s)/100)", nombresPVPFormateados),
                        rd(costoConImpuestos),
                        String.format("%s / (1 - (%s)/100) = %s",
                                fmt(costoConImpuestosAntesPVP),
                                detalleConceptosPVP.isEmpty() ? fmt(gastosSobrePVPTotal) : detalleConceptosPVP,
                                fmt(costoConImpuestos))));
            }
        }

        BigDecimal pvpBase = costoConImpuestos;

        // Paso 10: RECARGO_CUPON y DESCUENTO (conceptos)
        List<CanalConcepto> conceptosRecargoCupon = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.RECARGO_CUPON)
                .collect(Collectors.toList());
        BigDecimal porcentajeRecargoCupon = calcularGastosPorcentaje(conceptosRecargoCupon);
        BigDecimal descuentoConceptos = obtenerDescuentoMaquina(conceptos);

        BigDecimal pvp = pvpBase;

        if (porcentajeRecargoCupon.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal recargoCuponFrac = porcentajeRecargoCupon.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP);
            BigDecimal denominadorRecargoCupon = BigDecimal.ONE.subtract(recargoCuponFrac);
            if (denominadorRecargoCupon.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("RECARGO_CUPON es >= 100%, lo cual es inválido");
            }
            pvp = pvp.divide(denominadorRecargoCupon, PRECISION_CALCULO, RoundingMode.HALF_UP);
            List<String> nombresConceptosRecargoCupon = obtenerNombresConceptos(conceptos, AplicaSobre.RECARGO_CUPON);
            String nombresRecargoCuponFormateados = formatearNombresConceptos(nombresConceptosRecargoCupon);
            String detalleConceptosRecargoCupon = formatearDetalleConceptos(conceptosRecargoCupon);
            pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                    "Aplicar RECARGO_CUPON",
                    String.format("PVP = PVP / (1 - (%s)/100)", nombresRecargoCuponFormateados),
                    rd(pvp),
                    String.format("%s / (1 - (%s)/100) = %s", fmt(pvpBase),
                            detalleConceptosRecargoCupon.isEmpty() ? fmt(porcentajeRecargoCupon)
                                    : detalleConceptosRecargoCupon,
                            fmt(pvp))));
        }

        if (descuentoConceptos.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal descuentoFrac = descuentoConceptos.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP);
            BigDecimal factorDescuento = BigDecimal.ONE.subtract(descuentoFrac);
            BigDecimal pvpAntesDescuento = pvp;
            pvp = pvp.multiply(factorDescuento);
            List<CanalConcepto> conceptosDescuento = conceptos.stream()
                    .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.DESCUENTO)
                    .collect(Collectors.toList());
            List<String> nombresConceptosDescuento = obtenerNombresConceptos(conceptos, AplicaSobre.DESCUENTO);
            String nombresDescuentoFormateados = formatearNombresConceptos(nombresConceptosDescuento);
            String detalleConceptosDescuento = formatearDetalleConceptos(conceptosDescuento);
            pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                    "Aplicar DESCUENTO",
                    String.format("PVP = PVP * (1 - (%s)/100)", nombresDescuentoFormateados),
                    rd(pvp),
                    String.format("%s * (1 - (%s)/100) = %s", fmt(pvpAntesDescuento),
                            detalleConceptosDescuento.isEmpty() ? fmt(descuentoConceptos) : detalleConceptosDescuento,
                            fmt(pvp))));
        }

        // Paso 11: Reglas de descuento
        BigDecimal descuentoTotal = obtenerDescuentoAplicable(producto, idCanal, pvp);
        if (descuentoTotal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal descuentoFrac = descuentoTotal.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP);
            BigDecimal denominadorDescuento = BigDecimal.ONE.subtract(descuentoFrac);
            if (denominadorDescuento.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("El descuento es >= 100%, lo cual es inválido");
            }
            BigDecimal pvpAntesDescuentoRegla = pvp;
            pvp = pvp.divide(denominadorDescuento, PRECISION_CALCULO, RoundingMode.HALF_UP);
            pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                    "Aplicar reglas de descuento",
                    String.format("PVP = PVP / (1 - DESCUENTO_REGLA/100)"),
                    rd(pvp),
                    String.format("%s / (1 - %s/100) = %s", fmt(pvpAntesDescuentoRegla), fmt(descuentoTotal), fmt(pvp))));
        }

        // Paso 12: Margen fijo
        BigDecimal margenFijo = obtenerMargenFijo(productoMargen, conceptos);
        if (margenFijo.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal pvpAntesMargenFijo = pvp;
            pvp = pvp.add(margenFijo);
            pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                    "Aplicar margen fijo",
                    "PVP = PVP + MARGEN_FIJO",
                    rd(pvp),
                    String.format("%s + %s = %s", fmt(pvpAntesMargenFijo), fmt(margenFijo), fmt(pvp))));
        }

        // Paso 13: INFLACION (concepto)
        List<CanalConcepto> conceptosInflacion = conceptos.stream()
                .filter(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.INFLACION)
                .collect(Collectors.toList());

        BigDecimal pvpSinPromocion = pvp.setScale(PRECISION_RESULTADO, RoundingMode.HALF_UP);

        if (!conceptosInflacion.isEmpty()) {
            BigDecimal porcentajeInflacion = calcularGastosPorcentaje(conceptosInflacion);
            if (porcentajeInflacion.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal divisor = BigDecimal.ONE.subtract(porcentajeInflacion.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP));
                if (divisor.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal pvpAntesInflacion = pvpSinPromocion;
                    pvpSinPromocion = pvpSinPromocion.divide(divisor, PRECISION_CALCULO, RoundingMode.HALF_UP)
                            .setScale(PRECISION_RESULTADO, RoundingMode.HALF_UP);
                    List<String> nombresConceptosInflacion = obtenerNombresConceptos(conceptos, AplicaSobre.INFLACION);
                    String nombresInflacionFormateados = formatearNombresConceptos(nombresConceptosInflacion);
                    String detalleConceptosInflacion = formatearDetalleConceptos(conceptosInflacion);
                    pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                            "Aplicar INFLACION",
                            String.format("PVP = PVP / (1 - (%s)/100)", nombresInflacionFormateados),
                            rd(pvpSinPromocion),
                            String.format("%s / (1 - (%s)/100) = %s", fmt(pvpAntesInflacion),
                                    detalleConceptosInflacion.isEmpty() ? fmt(porcentajeInflacion)
                                            : detalleConceptosInflacion,
                                    fmt(pvpSinPromocion))));
                }
            }
        } else {
            pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                    "PVP sin promociones",
                    "PVP_SIN_PROMOCION",
                    rd(pvpSinPromocion),
                    String.format("PVP sin promociones: $%s", fmt(pvpSinPromocion))));
        }

        // Paso 14: Promociones (solo si el canal tiene concepto PROMOCION habilitado)
        BigDecimal pvpInflado = aplicarPromocionSinInflacion(producto.getId(), idCanal,
                pvpSinPromocion, usaPromociones);
        pvpInflado = pvpInflado.setScale(PRECISION_RESULTADO, RoundingMode.HALF_UP);

        if (pvpInflado.compareTo(pvpSinPromocion) != 0) {
            Optional<ProductoCanalPromocion> promocionOpt = productoCanalPromocionRepository
                    .findByProductoIdAndCanalId(producto.getId(), idCanal);

            if (promocionOpt.isPresent() && promocionOpt.get().getActiva() != null && promocionOpt.get().getActiva()) {
                Promocion promocionMaestra = promocionOpt.get().getPromocion();
                if (promocionMaestra != null) {
                    TipoPromocionTabla tipo = promocionMaestra.getTipo();
                    BigDecimal valor = promocionMaestra.getValor();
                    String tipoPromocion = tipo.toString();
                    pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                            "Aplicar promoción",
                            String.format("PVP_INFLADO = aplicarPromocion(PVP_SIN_PROMOCION, tipo=%s, valor=%s)",
                                    tipoPromocion, fmt(valor)),
                            rd(pvpInflado),
                            String.format("Promoción %s: %s → %s", tipoPromocion, fmt(pvpSinPromocion), fmt(pvpInflado))));
                }
            }
        }

        // Construir fórmula general dinámicamente
        StringBuilder formulaGeneral = new StringBuilder("PVP = ");
        formulaGeneral.append("((COSTO");
        if (gastosSobreCostoTotal.compareTo(BigDecimal.ZERO) > 0) {
            List<String> nombresCosto = obtenerNombresConceptos(conceptos, AplicaSobre.COSTO);
            formulaGeneral.append(" * (1 + ").append(formatearNombresConceptos(nombresCosto)).append("/100)");
        }
        formulaGeneral.append(" * (1 + GANANCIA/100)");
        if (!conceptosEnvio.isEmpty()) {
            formulaGeneral.append(" + ENVIO");
        }
        formulaGeneral.append(") * IMP");
        if (gastosSobreCostoIvaTotal.compareTo(BigDecimal.ZERO) > 0) {
            List<String> nombresCostoIva = obtenerNombresConceptos(conceptos, AplicaSobre.COSTO_IVA);
            formulaGeneral.append(" * (1 + ").append(formatearNombresConceptos(nombresCostoIva)).append("/100)");
        }
        if (aplicarCuotas) {
            formulaGeneral.append(" / (1 - GTML[%]/100)");
        } else if (gastosSobrePVPTotal.compareTo(BigDecimal.ZERO) > 0) {
            List<String> nombresPVP = obtenerNombresConceptos(conceptos, AplicaSobre.PVP);
            formulaGeneral.append(" / (1 - ").append(formatearNombresConceptos(nombresPVP)).append("/100)");
        }
        if (porcentajeRecargoCupon.compareTo(BigDecimal.ZERO) > 0) {
            List<String> nombresRecargoCupon = obtenerNombresConceptos(conceptos, AplicaSobre.RECARGO_CUPON);
            formulaGeneral.append(" / (1 - ").append(formatearNombresConceptos(nombresRecargoCupon)).append("/100)");
        }
        if (descuentoConceptos.compareTo(BigDecimal.ZERO) > 0) {
            List<String> nombresDescuento = obtenerNombresConceptos(conceptos, AplicaSobre.DESCUENTO);
            formulaGeneral.append(" * (1 - ").append(formatearNombresConceptos(nombresDescuento)).append("/100)");
        }
        if (descuentoTotal.compareTo(BigDecimal.ZERO) > 0) {
            formulaGeneral.append(" / (1 - DESCUENTO_REGLA/100)");
        }
        if (margenFijo.compareTo(BigDecimal.ZERO) > 0) {
            formulaGeneral.append(" + MARGEN_FIJO");
        }
        if (!conceptosInflacion.isEmpty()) {
            List<String> nombresInflacion = obtenerNombresConceptos(conceptos, AplicaSobre.INFLACION);
            formulaGeneral.append(" / (1 - ").append(formatearNombresConceptos(nombresInflacion)).append("/100)");
        }
        formulaGeneral.append(" + PROMOCIONES");

        Canal canal = canalRepository.findById(idCanal)
                .orElseThrow(() -> new NotFoundException("Canal no encontrado"));

        return new FormulaCalculoDTO(
                canal.getCanal(),
                numeroCuotas,
                formulaGeneral.toString(),
                pasos,
                pvpInflado);
    }

    /**
     * Calcula el precio basándose en el PVP del canal base.
     * Se usa cuando el canal tiene un canalBase configurado.
     * Si hay conceptos SOBRE_PVP_BASE, se aplican como multiplicadores.
     * Si no hay conceptos, el PVP es igual al del canal base.
     * <p>
     * Fórmula: PVP = PVP_CANAL_BASE * (1 + porcentaje1/100) * (1 + porcentaje2/100) * ...
     * <p>
     * El costoTotal se hereda directamente del canal base.
     *
     * @param producto       El producto
     * @param productoMargen La relación producto-canal (no usado, pero mantenido por consistencia)
     * @param conceptos      Lista de conceptos del canal (se filtran SOBRE_PVP_BASE)
     * @param idCanal        ID del canal actual
     * @param numeroCuotas   Número de cuotas (null si contado)
     * @return DTO con el precio calculado
     */
    private PrecioCalculadoDTO calcularPrecioSobrePvpBase(
            Producto producto,
            ProductoMargen productoMargen,
            List<CanalConcepto> conceptos,
            Integer idCanal,
            Integer numeroCuotas) {


        // Obtener el canal actual para acceder al canal base
        Canal canalActual = canalRepository.findById(idCanal)
                .orElseThrow(() -> new NotFoundException("Canal no encontrado: " + idCanal));

        if (canalActual.getCanalBase() == null) {
            throw new BadRequestException(
                    "El canal '" + canalActual.getCanal() + "' no tiene canal base configurado");
        }

        Integer idCanalBase = canalActual.getCanalBase().getId();

        // Calcular el precio del canal base en tiempo real (llamada recursiva)
        PrecioCalculadoDTO precioBase = calcularPrecioCanal(producto.getId(), idCanalBase, null);

        BigDecimal pvpCanalBase = precioBase.pvp();
        if (pvpCanalBase == null || pvpCanalBase.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("El PVP del canal base es inválido (null o <= 0)");
        }

        // El costoTotal se hereda del canal base (ya incluye PROVEEDOR_FIN, ENVIO, etc.)
        BigDecimal costoTotal = precioBase.costoTotal();

        // Filtrar conceptos SOBRE_PVP_BASE
        List<CanalConcepto> conceptosSobrePvpBase = conceptos.stream()
                .filter(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.SOBRE_PVP_BASE)
                .collect(Collectors.toList());

        // Aplicar los factores de cada concepto SOBRE_PVP_BASE
        // PVP = PVP_BASE * (1 + factor1/100) * (1 + factor2/100) * ...
        BigDecimal pvp = pvpCanalBase;
        for (CanalConcepto cc : conceptosSobrePvpBase) {
            BigDecimal porcentaje = cc.getConcepto().getPorcentaje();
            if (porcentaje != null) {
                BigDecimal factor = BigDecimal.ONE.add(porcentaje.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP));
                pvp = pvp.multiply(factor);
            }
        }

        pvp = pvp.setScale(PRECISION_RESULTADO, RoundingMode.HALF_UP);

        // Calcular ganancia absoluta: pvp - costoTotal
        BigDecimal gananciaAbs = pvp.subtract(costoTotal)
                .setScale(PRECISION_RESULTADO, RoundingMode.HALF_UP);

        // Calcular gananciaPorcentaje: (gananciaAbs / pvp) * 100
        BigDecimal gananciaPorcentaje = BigDecimal.ZERO;
        if (pvp.compareTo(BigDecimal.ZERO) > 0) {
            gananciaPorcentaje = gananciaAbs.multiply(CIEN)
                    .divide(pvp, PRECISION_RESULTADO, RoundingMode.HALF_UP);
        }

        // Calcular markupPorcentaje: (gananciaAbs / costoTotal) * 100
        BigDecimal markupPorcentaje = BigDecimal.ZERO;
        if (costoTotal.compareTo(BigDecimal.ZERO) > 0) {
            markupPorcentaje = gananciaAbs.multiply(CIEN)
                    .divide(costoTotal, PRECISION_RESULTADO, RoundingMode.HALF_UP);
        }

        return new PrecioCalculadoDTO(
                idCanal,
                canalActual.getCanal(),
                numeroCuotas,
                pvp,        // pvp sin promoción
                pvp,        // pvp con promoción (igual en este caso)
                costoTotal,
                gananciaAbs,
                gananciaPorcentaje,
                markupPorcentaje,
                LocalDateTime.now());
    }

    /**
     * Genera la fórmula de cálculo para canales basados en PVP del canal base.
     * Si hay conceptos SOBRE_PVP_BASE, se muestran como multiplicadores.
     * Si no hay conceptos, muestra que el PVP es igual al del canal base.
     *
     * @param producto              El producto
     * @param conceptosSobrePvpBase Conceptos con SOBRE_PVP_BASE (puede ser vacío)
     * @param idCanal               ID del canal actual
     * @return DTO con los pasos de la fórmula
     */
    private FormulaCalculoDTO generarFormulaSobrePvpBase(
            Producto producto,
            List<CanalConcepto> conceptosSobrePvpBase,
            Integer idCanal) {

        List<FormulaCalculoDTO.PasoCalculo> pasos = new java.util.ArrayList<>();
        int pasoNumero = 1;

        // Obtener el canal actual y su canal base
        Canal canalActual = canalRepository.findById(idCanal).orElse(null);
        String nombreCanalBase = "CANAL_BASE";
        Integer idCanalBase = null;

        if (canalActual != null && canalActual.getCanalBase() != null) {
            nombreCanalBase = canalActual.getCanalBase().getCanal();
            idCanalBase = canalActual.getCanalBase().getId();
        }

        // Paso 1: Calcular PVP del canal base en tiempo real
        BigDecimal pvpCanalBase = BigDecimal.ZERO;
        if (idCanalBase != null) {
            PrecioCalculadoDTO precioBase = calcularPrecioCanal(producto.getId(), idCanalBase, null);
            pvpCanalBase = precioBase.pvp();
        }

        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                "PVP del canal base",
                String.format("PVP_%s", nombreCanalBase),
                rd(pvpCanalBase),
                String.format("Calculado del canal base: $%s", fmt(pvpCanalBase))));

        // Paso 2: Aplicar factores (si los hay)
        BigDecimal pvp = pvpCanalBase;
        for (CanalConcepto cc : conceptosSobrePvpBase) {
            String nombreConcepto = cc.getConcepto().getConcepto();
            BigDecimal porcentaje = cc.getConcepto().getPorcentaje();
            if (porcentaje != null) {
                BigDecimal factor = BigDecimal.ONE.add(porcentaje.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP));
                BigDecimal pvpAnterior = pvp;
                pvp = pvp.multiply(factor);

                String signo = porcentaje.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
                pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                        String.format("Aplicar %s (%s%s%%)", nombreConcepto, signo, fmt(porcentaje)),
                        String.format("PVP = PVP * (1 + %s/100)", fmt(porcentaje)),
                        rd(pvp),
                        String.format("%s * %s = %s", fmt(pvpAnterior), fmt(factor), fmt(pvp))));
            }
        }

        pvp = pvp.setScale(PRECISION_RESULTADO, RoundingMode.HALF_UP);

        // Paso final: PVP resultante
        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                "PVP Final",
                "PVP",
                rd(pvp),
                String.format("$%s", fmt(pvp))));

        // Fórmula general depende de si hay factores o no
        String formulaGeneral = conceptosSobrePvpBase.isEmpty()
                ? String.format("PVP = PVP_%s", nombreCanalBase)
                : String.format("PVP = PVP_%s * factores", nombreCanalBase);

        return new FormulaCalculoDTO(
                canalActual != null ? canalActual.getCanal() : "Canal",
                null, // sin cuotas
                formulaGeneral,
                pasos,
                pvp);
    }

    private BigDecimal calcularGastosPorcentaje(List<CanalConcepto> conceptos) {
        return conceptos.stream()
                .map(cc -> cc.getConcepto().getPorcentaje())
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Obtiene el porcentaje de cuota aplicable.
     *
     * @param idCanal      ID del canal
     * @param numeroCuotas Número de cuotas
     * @return Porcentaje de cuota o null si no se encuentra
     */
    private BigDecimal obtenerPorcentajeCuota(Integer idCanal, Integer numeroCuotas) {
        List<CanalConceptoCuota> cuotasCanal = canalConceptoCuotaRepository.findByCanalIdAndCuotas(idCanal, numeroCuotas);

        return cuotasCanal.stream()
                .findFirst()
                .map(CanalConceptoCuota::getPorcentaje)
                .orElse(null);
    }

    /**
     * Obtiene el descuento de máquina desde los conceptos. Busca dinámicamente
     * conceptos con aplica_sobre='DESCUENTO' en la lista ya filtrada por reglas
     * EXCLUIR.
     * <p>
     * NOTA: Los conceptos ya vienen filtrados por canal_concepto_regla desde
     * obtenerConceptosAplicables(). Para productos máquinas, los conceptos con
     * regla EXCLUIR (es_maquina=false) ya fueron excluidos de la lista. Este
     * método busca dinámicamente cualquier concepto con
     * aplica_sobre='DESCUENTO' en la lista filtrada, sin hardcodear nombres de
     * conceptos específicos. Si no existe (porque fue excluido), retorna cero.
     *
     * @param conceptos Lista de CanalConcepto ya filtrada por reglas EXCLUIR
     *                  (solo incluye conceptos aplicables)
     * @return Porcentaje total de descuento, o BigDecimal.ZERO si no hay
     * conceptos con aplica_sobre='DESCUENTO'
     */
    private BigDecimal obtenerDescuentoMaquina(List<CanalConcepto> conceptos) {
        return conceptos.stream()
                .filter(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.DESCUENTO)
                .map(cc -> cc.getConcepto().getPorcentaje())
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Obtiene el descuento aplicable al producto según las reglas de descuento
     * del canal. Retorna el descuento porcentual de la regla de mayor prioridad
     * que cumpla todas las condiciones.
     *
     * @param producto El producto al que se aplicará el descuento
     * @param canalId  El ID del canal
     * @param pvpBase  El PVP base antes de aplicar descuentos (para validar
     *                 monto mínimo)
     * @return El porcentaje de descuento aplicable (0 si no hay descuento
     * aplicable)
     */
    private BigDecimal obtenerDescuentoAplicable(Producto producto, Integer canalId, BigDecimal pvpBase) {
        // Obtener todas las reglas activas del canal, ordenadas por prioridad
        List<ReglaDescuento> reglas = reglaDescuentoRepository
                .findByCanalIdAndActivoTrueOrderByPrioridadAsc(canalId);

        // Obtener los catálogos del producto
        List<Integer> catalogosProducto = productoCatalogoRepository
                .findByProductoId(producto.getId())
                .stream()
                .map(pc -> pc.getCatalogo().getId())
                .collect(Collectors.toList());

        // Buscar la primera regla que cumpla todas las condiciones
        for (ReglaDescuento regla : reglas) {
            // Verificar monto mínimo
            if (pvpBase.compareTo(regla.getMontoMinimo()) < 0) {
                continue;
            }

            // Verificar catálogo (si la regla tiene catálogo, el producto debe estar en ese catálogo)
            if (regla.getCatalogo() != null) {
                if (!catalogosProducto.contains(regla.getCatalogo().getId())) {
                    continue;
                }
            }

            // Verificar clasificación general (si la regla tiene clasifGral, debe
            // coincidir)
            if (regla.getClasifGral() != null) {
                if (producto.getClasifGral() == null
                        || !producto.getClasifGral().getId().equals(regla.getClasifGral().getId())) {
                    continue;
                }
            }

            // Verificar clasificación gastro (si la regla tiene clasifGastro, debe
            // coincidir)
            if (regla.getClasifGastro() != null) {
                if (producto.getClasifGastro() == null
                        || !producto.getClasifGastro().getId().equals(regla.getClasifGastro().getId())) {
                    continue;
                }
            }

            // Si llegamos aquí, la regla aplica
            return regla.getDescuentoPorcentaje();
        }

        // No se encontró ninguna regla aplicable
        return BigDecimal.ZERO;
    }

    // ====================================================
    // HELPERS
    // ====================================================
    private Producto obtenerProducto(Integer idProducto) {
        // Usa JOIN FETCH para cargar relaciones necesarias para evaluar reglas de canal_concepto_regla
        return productoRepository.findByIdConRelacionesParaReglas(idProducto)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado"));
    }

    private ProductoMargen obtenerProductoMargen(Integer idProducto) {
        return productoMargenRepository
                .findByProductoId(idProducto)
                .orElseThrow(() -> new NotFoundException("No existe configuración de márgenes para este producto"));
    }

    /**
     * Obtiene el margen porcentual según los conceptos del canal.
     * - Si tiene MARGEN_MAYORISTA → usa margenMayorista
     * - Si tiene MARGEN_MINORISTA → usa margenMinorista
     */
    private BigDecimal obtenerMargenPorcentaje(ProductoMargen productoMargen, List<CanalConcepto> conceptos) {
        for (CanalConcepto cc : conceptos) {
            if (cc.getConcepto() != null) {
                if (cc.getConcepto().getAplicaSobre() == AplicaSobre.MARGEN_MAYORISTA) {
                    return productoMargen.getMargenMayorista() != null ? productoMargen.getMargenMayorista() : BigDecimal.ZERO;
                }
                if (cc.getConcepto().getAplicaSobre() == AplicaSobre.MARGEN_MINORISTA) {
                    return productoMargen.getMargenMinorista() != null ? productoMargen.getMargenMinorista() : BigDecimal.ZERO;
                }
            }
        }
        // Si no tiene ninguno de los conceptos, retorna ZERO
        return BigDecimal.ZERO;
    }

    /**
     * Obtiene el margen fijo según los conceptos del canal.
     * - Si tiene MARGEN_MAYORISTA → usa margenFijoMayorista
     * - Si tiene MARGEN_MINORISTA → usa margenFijoMinorista
     */
    private BigDecimal obtenerMargenFijo(ProductoMargen productoMargen, List<CanalConcepto> conceptos) {
        for (CanalConcepto cc : conceptos) {
            if (cc.getConcepto() != null) {
                if (cc.getConcepto().getAplicaSobre() == AplicaSobre.MARGEN_MAYORISTA) {
                    return productoMargen.getMargenFijoMayorista() != null ? productoMargen.getMargenFijoMayorista() : BigDecimal.ZERO;
                }
                if (cc.getConcepto().getAplicaSobre() == AplicaSobre.MARGEN_MINORISTA) {
                    return productoMargen.getMargenFijoMinorista() != null ? productoMargen.getMargenFijoMinorista() : BigDecimal.ZERO;
                }
            }
        }
        // Si no tiene ninguno de los conceptos, retorna ZERO
        return BigDecimal.ZERO;
    }

    /**
     * Obtiene todos los conceptos de gasto que aplican al canal según los
     * filtros.
     * <p>
     * Sistema unificado: todos los conceptos se asocian a canales a través de
     * canal_concepto.
     * <p>
     * Lógica de filtrado: - Canal: Un concepto aplica a un canal si está
     * asociado en canal_concepto - Jerarquía de canales: Si un concepto está
     * asignado al canal padre (ej: NUBE), también aplica a todos sus canales
     * hijos (ej: KT HOGAR, KT GASTRO) Nota: Las cuotas ahora se manejan a
     * través de canal_concepto_cuota, no a través del campo cuotas en
     * conceptos_gastos
     * <p>
     * REGLAS DE CANAL_CONCEPTO_REGLA: - Si tipo_regla = INCLUIR: el concepto
     * SOLO aplica si el producto cumple TODAS las condiciones - Si tipo_regla =
     * EXCLUIR: el concepto NO aplica si el producto cumple ALGUNA condición -
     * Condiciones disponibles: id_tipo, id_clasif_gral, id_clasif_gastro,
     * id_marca, es_maquina - es_maquina: filtra por máquina/no máquina
     * (true=solo máquinas, false=solo no máquinas, NULL=no filtra)
     * <p>
     * ENFOQUE RECOMENDADO PARA KT GASTRO (usar EXCLUIR):
     * <p>
     * 1. Conceptos comunes (no requieren reglas): - MARKETING, EMBALAJE,
     * GASTONUBE, COMISION ML (aplica_sobre='PVP') - IIBB (aplica_sobre='IMP')
     * Estos conceptos aplican a todos los productos del canal.
     * <p>
     * 2. DESCUENTO_MAQUINA (solo para máquinas): - Debe estar en canal_concepto
     * para KT GASTRO - Debe tener regla EXCLUIR con es_maquina=false (excluir
     * cuando NO es máquina) Ejemplo: tipo_regla='EXCLUIR', es_maquina=false
     * Resultado: Se excluye cuando producto.clasif_gastro.es_maquina = false →
     * Solo aplica a máquinas (es_maquina = true)
     * <p>
     * NOTA: REL_ML_KTG se usa para KT GASTRO NO MÁQUINA como concepto con
     * aplica_sobre='REDUCE_MARGEN_PTS' que reduce la ganancia restando directamente.
     * Debe tener regla EXCLUIR con es_maquina=true para que solo aplique a no máquinas.
     * <p>
     * NOTA: Los valores de aplica_sobre incluyen:
     * - AUMENTA_MARGEN_PTS: Suma puntos porcentuales directamente al margen (GAN.MIN.ML + porcentaje)
     * Ejemplo: Si GAN.MIN.ML = 60% y porcentaje = 25%, entonces ganancia = 60% + 25% = 85%
     * - REDUCE_MARGEN_PTS: Resta puntos porcentuales directamente del margen (GAN.MIN.ML - porcentaje)
     * Ejemplo: Si GAN.MIN.ML = 60% y porcentaje = 20%, entonces ganancia = 60% - 20% = 40%
     * - AUMENTA_MARGEN_PROP: Aumenta el margen proporcionalmente (MARGEN * (1 + porcentaje/100))
     * Ejemplo: Si GAN.MIN.ML = 60% y porcentaje = 10%, entonces ganancia = 60% * 1.10 = 66%
     * - REDUCE_MARGEN_PROP: Reduce el margen proporcionalmente (MARGEN * (1 - porcentaje/100))
     * Ejemplo: Si GAN.MIN.ML = 60% y porcentaje = 10%, entonces ganancia = 60% * 0.90 = 54%
     * NOTA: Esta aplicación es consistente para todos los canales.
     * <p>
     * NOTA: Todos los conceptos (comunes y específicos) deben estar en
     * canal_concepto. Las reglas EXCLUIR filtran cuáles conceptos NO aplican
     * según las condiciones del producto. Los métodos de cálculo buscan
     * dinámicamente conceptos por aplica_sobre sin hardcodear qué buscar.
     *
     * @param idCanal      ID del canal para filtrar conceptos
     * @param numeroCuotas Número de cuotas (parámetro mantenido por
     *                     compatibilidad, pero ya no se usa para filtrar conceptos)
     * @param producto     El producto para aplicar las reglas de
     *                     canal_concepto_regla
     * @return Lista de conceptos de gasto que aplican según los filtros
     */
    private List<ConceptoGasto> obtenerConceptosAplicables(Integer idCanal, Integer numeroCuotas,
                                                           Producto producto) {
        // Obtener el canal actual para acceder a su canal padre (canalBase)
        Canal canalActual = canalRepository.findById(idCanal).orElse(null);
        final Integer idCanalPadre = (canalActual != null && canalActual.getCanalBase() != null)
                ? canalActual.getCanalBase().getId()
                : null;

        // Obtener conceptos asociados al canal a través de canal_concepto
        // Nota: NO se heredan conceptos del canal padre, cada canal tiene sus propios conceptos
        List<CanalConcepto> conceptosPorCanal = canalConceptoRepository.findByCanalId(idCanal);

        // Obtener las reglas del canal
        List<CanalConceptoRegla> reglasCanal = canalConceptoReglaRepository.findByCanalId(idCanal);

        // Extraer los conceptos únicos que aplican al canal
        return conceptosPorCanal.stream()
                .map(CanalConcepto::getConcepto)
                .filter(concepto -> {
                    // APLICAR REGLAS DE CANAL_CONCEPTO_REGLA
                    // Buscar reglas que afecten a este concepto
                    List<CanalConceptoRegla> reglasConcepto = reglasCanal.stream()
                            .filter(regla -> regla.getConcepto().getId().equals(concepto.getId()))
                            .collect(Collectors.toList());

                    for (CanalConceptoRegla regla : reglasConcepto) {
                        boolean cumpleCondiciones = cumpleCondicionesRegla(regla, producto);

                        if (regla.getTipoRegla() == TipoRegla.INCLUIR) {
                            // INCLUIR: el concepto SOLO aplica si cumple TODAS las condiciones
                            if (!cumpleCondiciones) {
                                return false;
                            }
                        } else if (regla.getTipoRegla() == TipoRegla.EXCLUIR) {
                            // EXCLUIR: el concepto NO aplica si cumple ALGUNA condición
                            if (cumpleCondiciones) {
                                return false;
                            }
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * Verifica si un producto cumple las condiciones de una regla.
     *
     * @param regla    La regla a verificar
     * @param producto El producto a evaluar
     * @return true si el producto cumple TODAS las condiciones especificadas en
     * la regla
     */
    private boolean cumpleCondicionesRegla(CanalConceptoRegla regla, Producto producto) {
        // Si la regla no tiene condiciones, no se aplica (retorna false)
        boolean tieneCondiciones = regla.getTipo() != null
                || regla.getClasifGral() != null
                || regla.getClasifGastro() != null
                || regla.getMarca() != null
                || regla.getEsMaquina() != null;

        if (!tieneCondiciones) {
            return false;
        }

        // Verificar tipo
        if (regla.getTipo() != null) {
            if (producto.getTipo() == null || !producto.getTipo().getId().equals(regla.getTipo().getId())) {
                return false;
            }
        }

        // Verificar clasificación general
        if (regla.getClasifGral() != null) {
            if (producto.getClasifGral() == null
                    || !producto.getClasifGral().getId().equals(regla.getClasifGral().getId())) {
                return false;
            }
        }

        // Verificar clasificación gastro
        if (regla.getClasifGastro() != null) {
            if (producto.getClasifGastro() == null
                    || !producto.getClasifGastro().getId().equals(regla.getClasifGastro().getId())) {
                return false;
            }
        }

        // Verificar marca
        if (regla.getMarca() != null) {
            if (producto.getMarca() == null || !producto.getMarca().getId().equals(regla.getMarca().getId())) {
                return false;
            }
        }

        // Verificar es_maquina si está especificado en la regla
        if (regla.getEsMaquina() != null) {
            Boolean esMaquinaProducto = producto.getClasifGastro() != null
                    ? producto.getClasifGastro().getEsMaquina()
                    : null;

            if (esMaquinaProducto == null || !regla.getEsMaquina().equals(esMaquinaProducto)) {
                return false;
            }
        }

        // Si llegamos aquí, el producto cumple TODAS las condiciones especificadas
        return true;
    }

    /**
     * Convierte una lista de ConceptoGasto a CanalConcepto para mantener
     * compatibilidad con el método calcularPrecioInterno que espera
     * List<CanalConcepto>.
     *
     * @param conceptos Lista de ConceptoGasto
     * @param idCanal   ID del canal para establecer en los objetos CanalConcepto
     *                  temporales
     * @return Lista de CanalConcepto (objetos temporales para compatibilidad)
     */
    private List<CanalConcepto> convertirConceptosACanalConcepto(List<ConceptoGasto> conceptos, Integer idCanal) {
        // Obtener las relaciones canal_concepto para estos conceptos y el canal
        List<CanalConcepto> relacionesExistentes = canalConceptoRepository.findByCanalId(idCanal);
        java.util.Map<Integer, CanalConcepto> mapaPorConceptoId = relacionesExistentes.stream()
                .collect(java.util.stream.Collectors.toMap(
                        cc -> cc.getConcepto().getId(),
                        cc -> cc,
                        (existing, replacement) -> existing));

        return conceptos.stream()
                .map(concepto -> {
                    // Si ya existe una relación en canal_concepto, usarla
                    CanalConcepto cc = mapaPorConceptoId.get(concepto.getId());
                    if (cc != null) {
                        return cc;
                    }
                    // Si no existe, crear una temporal para compatibilidad con
                    // calcularPrecioInterno
                    CanalConcepto ccTemp = new CanalConcepto();
                    ccTemp.setConcepto(concepto);
                    ar.com.leo.super_master_backend.dominio.canal.entity.Canal canal = new ar.com.leo.super_master_backend.dominio.canal.entity.Canal();
                    canal.setId(idCanal);
                    ccTemp.setCanal(canal);
                    return ccTemp;
                })
                .collect(Collectors.toList());
    }

    /**
     * Obtiene los nombres de los conceptos filtrados por aplicaSobre.
     *
     * @param conceptos   Lista de CanalConcepto
     * @param aplicaSobre Tipo de aplicación sobre el cual filtrar
     * @return Lista de nombres de conceptos (campo concepto de ConceptoGasto)
     */
    private List<String> obtenerNombresConceptos(List<CanalConcepto> conceptos, AplicaSobre aplicaSobre) {
        return conceptos.stream()
                .filter(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == aplicaSobre)
                .map(cc -> cc.getConcepto().getConcepto())
                .filter(nombre -> nombre != null && !nombre.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Formatea una lista de nombres de conceptos concatenándolos con " + ".
     *
     * @param nombres Lista de nombres de conceptos
     * @return String con los nombres concatenados, o cadena vacía si la lista
     * está vacía
     */
    private String formatearNombresConceptos(List<String> nombres) {
        if (nombres == null || nombres.isEmpty()) {
            return "";
        }
        return String.join(" + ", nombres);
    }

    /**
     * Formatea el detalle de conceptos mostrando cada concepto con su porcentaje individual.
     *
     * @param conceptos Lista de conceptos con sus porcentajes
     * @return String con el detalle formateado (ej: "NUBE: 5.00% + MARKETING: 2.50%")
     */
    private String formatearDetalleConceptos(List<CanalConcepto> conceptos) {
        if (conceptos == null || conceptos.isEmpty()) {
            return "";
        }
        return conceptos.stream()
                .filter(cc -> cc.getConcepto() != null && cc.getConcepto().getConcepto() != null)
                .map(cc -> String.format("%s: %s%%",
                        cc.getConcepto().getConcepto(),
                        fmt(cc.getConcepto().getPorcentaje())))
                .collect(Collectors.joining(" + "));
    }

    /**
     * Redondea un BigDecimal a 2 decimales para mostrar en la fórmula.
     */
    private BigDecimal rd(BigDecimal value) {
        if (value == null) return null;
        return value.setScale(PRECISION_RESULTADO, RoundingMode.HALF_UP);
    }

    /**
     * Formatea un BigDecimal a string con 2 decimales.
     */
    private String fmt(BigDecimal value) {
        if (value == null) return "null";
        return value.setScale(PRECISION_RESULTADO, RoundingMode.HALF_UP).toString();
    }

    /**
     * Aplica promociones sin incluir porcentaje_inflacion (que ahora es un concepto).
     * Solo aplica promociones de producto_canal_promocion si el canal tiene habilitadas las promociones.
     *
     * @param productoId     ID del producto
     * @param canalId        ID del canal
     * @param pvp            Precio calculado antes de aplicar promociones
     * @param usaPromociones true si el canal tiene un concepto con aplicaSobre=PROMOCION
     * @return Precio con promociones aplicadas (o el mismo pvp si no usa promociones)
     */
    private BigDecimal aplicarPromocionSinInflacion(Integer productoId, Integer canalId, BigDecimal pvp, boolean usaPromociones) {
        BigDecimal resultado = pvp;

        // Si el canal no tiene habilitadas las promociones, retornar el pvp sin cambios
        if (!usaPromociones) {
            return resultado;
        }

        // Aplicar promoción de producto_canal_promocion (si existe y está activa)
        Optional<ProductoCanalPromocion> promocionOpt = Optional.empty();
        if (canalId != null) {
            promocionOpt = productoCanalPromocionRepository
                    .findByProductoIdAndCanalId(productoId, canalId);
        }

        if (promocionOpt.isPresent()) {
            ProductoCanalPromocion promocion = promocionOpt.get();

            // Verificar que la promoción esté activa
            if (promocion.getActiva() != null && promocion.getActiva()) {
                // Verificar rango de fechas si está configurado
                LocalDate hoy = LocalDate.now();
                boolean fechaValida = true;
                if (promocion.getFechaDesde() != null && hoy.isBefore(promocion.getFechaDesde())) {
                    fechaValida = false; // Promoción aún no iniciada
                }
                if (promocion.getFechaHasta() != null && hoy.isAfter(promocion.getFechaHasta())) {
                    fechaValida = false; // Promoción ya expiró
                }

                if (fechaValida) {
                    // Obtener la promoción de la tabla maestra
                    Promocion promocionMaestra = promocion.getPromocion();
                    if (promocionMaestra != null) {
                        TipoPromocionTabla tipo = promocionMaestra.getTipo();
                        BigDecimal valor = promocionMaestra.getValor();

                        switch (tipo) {
                            case MULTIPLICADOR:
                                // Multiplicador: precio * valor
                                // Ejemplo: valor = 1.1 multiplica el precio por 1.1 (aumenta 10%)
                                if (valor.compareTo(BigDecimal.ZERO) > 0) {
                                    resultado = resultado.multiply(valor);
                                }
                                break;

                            case DESCUENTO_PORC:
                                // Descuento/Incremento porcentual según fórmula Excel: PVP / (1 - PROMO)
                                // Si valor = 30 (30%): precio / (1 - 0.30) = precio / 0.70 (incrementa ~42.86%)
                                // Si valor = 10 (10%): precio / (1 - 0.10) = precio / 0.90 (incrementa ~11.11%)
                                // Fórmula: resultado = resultado / (1 - valor/100)
                                if (valor.compareTo(BigDecimal.ZERO) > 0 && valor.compareTo(CIEN) < 0) {
                                    BigDecimal promocionFrac = valor.divide(CIEN, PRECISION_CALCULO, RoundingMode.HALF_UP);
                                    BigDecimal denominador = BigDecimal.ONE.subtract(promocionFrac);
                                    if (denominador.compareTo(BigDecimal.ZERO) > 0) {
                                        resultado = resultado.divide(denominador, PRECISION_CALCULO, RoundingMode.HALF_UP);
                                    }
                                }
                                break;

                            case DIVISOR:
                                // Divisor: precio / valor
                                // Ejemplo: valor = 0.9 divide el precio por 0.9 (equivalente a multiplicar por 1.11)
                                if (valor.compareTo(BigDecimal.ZERO) > 0) {
                                    resultado = resultado.divide(valor, PRECISION_CALCULO, RoundingMode.HALF_UP);
                                }
                                break;

                            case PRECIO_FIJO:
                                // Precio fijo: establecer precio directamente
                                // Ejemplo: valor = 100 significa precio fijo de $100
                                if (valor.compareTo(BigDecimal.ZERO) > 0) {
                                    resultado = valor;
                                }
                                break;

                            default:
                                // Tipo desconocido, no modificar resultado
                                break;
                        }
                    }
                }
            }
        }

        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public FormulaCalculoDTO obtenerFormulaCalculo(Integer idProducto, Integer idCanal, Integer numeroCuotas) {
        Producto producto = obtenerProducto(idProducto);
        ProductoMargen productoMargen = obtenerProductoMargen(idProducto);

        // Obtener todos los conceptos que aplican al canal según los filtros
        // Sistema unificado: todos los conceptos se obtienen dinámicamente desde canal_concepto
        // y se filtran por reglas de canal_concepto_regla según el producto
        List<ConceptoGasto> conceptos = obtenerConceptosAplicables(idCanal, numeroCuotas, producto);

        List<CanalConcepto> conceptosCanal = convertirConceptosACanalConcepto(conceptos, idCanal);

        return generarFormulaUnificado(producto, productoMargen, conceptosCanal, numeroCuotas, idCanal);
    }

    // ====================================================
    // CÁLCULO PARA TODAS LAS CUOTAS
    // ====================================================

    @Override
    public List<PrecioCalculadoDTO> calcularPrecioCanalTodasCuotas(Integer idProducto, Integer idCanal) {
        // Obtener todas las cuotas configuradas para el canal (incluye cuotas=0 para transferencia/contado)
        List<Integer> cuotasCanal = canalConceptoCuotaRepository.findDistinctCuotasByCanalId(idCanal);

        // Solo calcular las cuotas configuradas en canal_concepto_cuota
        List<PrecioCalculadoDTO> precios = new java.util.ArrayList<>();

        for (Integer cuotas : cuotasCanal) {
            precios.add(calcularPrecioCanal(idProducto, idCanal, cuotas));
        }

        return precios;
    }

    @Override
    @Transactional
    public CanalPreciosDTO recalcularYGuardarPrecioCanalTodasCuotas(Integer idProducto, Integer idCanal) {
        // Obtener todas las cuotas configuradas para el canal (incluye cuotas=0 para transferencia/contado)
        List<Integer> cuotasCanal = canalConceptoCuotaRepository.findDistinctCuotasByCanalId(idCanal);

        // Obtener descripciones de cada cuota para el canal
        Map<Integer, String> descripcionesCuotas = canalConceptoCuotaRepository.findByCanalId(idCanal).stream()
                .collect(java.util.stream.Collectors.toMap(
                        CanalConceptoCuota::getCuotas,
                        c -> c.getDescripcion() != null ? c.getDescripcion() : "",
                        (a, b) -> a
                ));

        // Solo calcular las cuotas configuradas en canal_concepto_cuota
        List<PrecioCalculadoDTO> preciosCalculados = new ArrayList<>();

        for (Integer cuotas : cuotasCanal) {
            preciosCalculados.add(recalcularYGuardarPrecioCanal(idProducto, idCanal, cuotas));
        }

        // Obtener info del canal del primer precio
        String canalNombre = preciosCalculados.isEmpty() ? null : preciosCalculados.get(0).canalNombre();

        // Convertir a PrecioDTO (sin canalId y canalNombre repetidos) + agregar descripcion
        List<PrecioDTO> precios = preciosCalculados.stream()
                .map(p -> new PrecioDTO(
                        p.cuotas(),
                        descripcionesCuotas.getOrDefault(p.cuotas(), ""),
                        p.pvp(),
                        p.pvpInflado(),
                        p.costoTotal(),
                        p.gananciaAbs(),
                        p.gananciaPorcentaje(),
                        p.markupPorcentaje(),
                        p.fechaUltimoCalculo()
                ))
                .toList();

        return new CanalPreciosDTO(idCanal, canalNombre, precios);
    }

    @Override
    public CanalPreciosDTO recalcularYGuardar(Integer idProducto, Integer idCanal, Integer cuotas) {
        // Validar que el canal existe
        Canal canal = canalRepository.findById(idCanal)
                .orElseThrow(() -> new NotFoundException("Canal no encontrado con ID: " + idCanal));

        // Validar que las cuotas existen para este canal (si se especificaron)
        if (cuotas != null) {
            List<Integer> cuotasDisponibles = canalConceptoCuotaRepository.findDistinctCuotasByCanalId(idCanal);
            if (!cuotasDisponibles.contains(cuotas)) {
                throw new NotFoundException(
                        "Cuotas " + cuotas + " no configuradas para el canal '" + canal.getCanal() + "'. " +
                        "Cuotas disponibles: " + cuotasDisponibles
                );
            }
        }

        if (cuotas == null) {
            return recalcularYGuardarPrecioCanalTodasCuotas(idProducto, idCanal);
        }

        // Calcular solo para las cuotas especificadas
        PrecioCalculadoDTO precioCalculado = recalcularYGuardarPrecioCanal(idProducto, idCanal, cuotas);

        // Obtener descripción de la cuota
        String descripcion = canalConceptoCuotaRepository.findByCanalId(idCanal).stream()
                .filter(c -> c.getCuotas().equals(cuotas))
                .map(c -> c.getDescripcion() != null ? c.getDescripcion() : "")
                .findFirst()
                .orElse("");

        PrecioDTO precioDTO = new PrecioDTO(
                precioCalculado.cuotas(),
                descripcion,
                precioCalculado.pvp(),
                precioCalculado.pvpInflado(),
                precioCalculado.costoTotal(),
                precioCalculado.gananciaAbs(),
                precioCalculado.gananciaPorcentaje(),
                precioCalculado.markupPorcentaje(),
                precioCalculado.fechaUltimoCalculo()
        );

        return new CanalPreciosDTO(idCanal, precioCalculado.canalNombre(), List.of(precioDTO));
    }

    @Override
    @Transactional
    public List<CanalPreciosDTO> recalcularProductoTodosCanales(Integer idProducto) {
        // Validar que el producto existe
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado con ID: " + idProducto));

        // Obtener todos los canales donde el producto tiene precios configurados (query optimizada)
        List<Integer> canalIds = productoCanalPrecioRepository.findDistinctCanalIdsByProductoId(idProducto);

        if (canalIds.isEmpty()) {
            throw new NotFoundException(
                    "El producto '" + producto.getDescripcion() + "' (ID: " + idProducto + ") no tiene canales configurados"
            );
        }

        // Recalcular para cada canal (todas las cuotas)
        return canalIds.stream()
                .map(idCanal -> recalcularYGuardarPrecioCanalTodasCuotas(idProducto, idCanal))
                .toList();
    }

    @Override
    @Transactional
    public List<CanalPreciosDTO> recalcularProductoTodosCanales(Integer idProducto, Integer cuotas) {
        // Validar que el producto existe
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado con ID: " + idProducto));

        // Obtener todos los canales donde el producto tiene precios configurados (query optimizada)
        List<Integer> canalIds = productoCanalPrecioRepository.findDistinctCanalIdsByProductoId(idProducto);

        if (canalIds.isEmpty()) {
            throw new NotFoundException(
                    "El producto '" + producto.getDescripcion() + "' (ID: " + idProducto + ") no tiene canales configurados"
            );
        }

        // Recalcular para cada canal solo las cuotas indicadas
        List<CanalPreciosDTO> resultados = new ArrayList<>();
        for (Integer idCanal : canalIds) {
            // Verificar si este canal tiene configuradas esas cuotas
            List<Integer> cuotasDisponibles = canalConceptoCuotaRepository.findDistinctCuotasByCanalId(idCanal);
            if (cuotasDisponibles.contains(cuotas)) {
                resultados.add(recalcularYGuardar(idProducto, idCanal, cuotas));
            }
        }

        if (resultados.isEmpty()) {
            throw new NotFoundException(
                    "Ningún canal del producto '" + producto.getDescripcion() + "' tiene configuradas " + cuotas + " cuotas"
            );
        }

        return resultados;
    }

    @Override
    @Transactional
    public int recalcularTodos() {
        // Obtener todos los productos que tienen márgenes configurados
        List<ProductoMargen> productosConMargenes = productoMargenRepository.findAll();
        List<Canal> todosLosCanales = canalRepository.findAll();

        int totalRecalculados = 0;

        for (ProductoMargen productoMargen : productosConMargenes) {
            // Ignorar productos sin costo
            BigDecimal costo = productoMargen.getProducto().getCosto();
            if (costo == null || costo.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Integer idProducto = productoMargen.getProducto().getId();

            for (Canal canal : todosLosCanales) {
                if (!tieneMargenValidoParaCanal(productoMargen, canal)) {
                    continue;
                }

                try {
                    CanalPreciosDTO resultado = recalcularYGuardarPrecioCanalTodasCuotas(idProducto, canal.getId());
                    totalRecalculados += resultado.precios().size();
                } catch (Exception e) {
                    // Log silencioso, continuar con el siguiente
                }
            }
        }

        return totalRecalculados;
    }

    /**
     * Verifica si un canal tiene margen válido (> 0) para un producto.
     */
    private boolean tieneMargenValidoParaCanal(ProductoMargen productoMargen, Canal canal) {
        List<CanalConcepto> conceptosCanal = canalConceptoRepository.findByCanalId(canal.getId());

        for (CanalConcepto cc : conceptosCanal) {
            if (cc.getConcepto() != null) {
                if (cc.getConcepto().getAplicaSobre() == AplicaSobre.MARGEN_MAYORISTA) {
                    BigDecimal margen = productoMargen.getMargenMayorista();
                    return margen != null && margen.compareTo(BigDecimal.ZERO) > 0;
                }
                if (cc.getConcepto().getAplicaSobre() == AplicaSobre.MARGEN_MINORISTA) {
                    BigDecimal margen = productoMargen.getMargenMinorista();
                    return margen != null && margen.compareTo(BigDecimal.ZERO) > 0;
                }
            }
        }

        return false;
    }

}
