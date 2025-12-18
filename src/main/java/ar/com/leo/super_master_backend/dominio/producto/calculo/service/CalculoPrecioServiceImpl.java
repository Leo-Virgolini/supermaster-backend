package ar.com.leo.super_master_backend.dominio.producto.calculo.service;

import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConcepto;
import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConceptoCuota;
import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConceptoRegla;
import ar.com.leo.super_master_backend.dominio.canal.entity.TipoCuota;
import ar.com.leo.super_master_backend.dominio.canal.entity.TipoRegla;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.entity.AplicaSobre;
import ar.com.leo.super_master_backend.dominio.producto.calculo.dto.PrecioCalculadoDTO;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanal;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanalPrecio;
import ar.com.leo.super_master_backend.dominio.canal.entity.Canal;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalConceptoCuotaRepository;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalConceptoReglaRepository;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalConceptoRepository;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalRepository;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.entity.ConceptoGasto;
import ar.com.leo.super_master_backend.dominio.producto.mla.repository.MlaRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalPrecioRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCatalogoRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoRepository;
import ar.com.leo.super_master_backend.dominio.regla_descuento.entity.ReglaDescuento;
import ar.com.leo.super_master_backend.dominio.regla_descuento.repository.ReglaDescuentoRepository;
import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import ar.com.leo.super_master_backend.dominio.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalculoPrecioServiceImpl implements CalculoPrecioService {

    private final ProductoRepository productoRepository;
    private final ProductoCanalRepository productoCanalRepository;
    private final ProductoCanalPrecioRepository productoCanalPrecioRepository;
    private final ReglaDescuentoRepository reglaDescuentoRepository;
    private final ProductoCatalogoRepository productoCatalogoRepository;
    private final MlaRepository mlaRepository;
    private final CanalRepository canalRepository;
    private final CanalConceptoRepository canalConceptoRepository;
    private final CanalConceptoReglaRepository canalConceptoReglaRepository;
    private final CanalConceptoCuotaRepository canalConceptoCuotaRepository;

    // ====================================================
    // API PÚBLICA
    // ====================================================

    @Override
    @Transactional(readOnly = true)
    public PrecioCalculadoDTO calcularPrecioCanal(Integer idProducto, Integer idCanal, Integer numeroCuotas) {
        Producto producto = obtenerProducto(idProducto);
        ProductoCanal productoCanal = obtenerProductoCanal(idProducto, idCanal);

        // Obtener todos los conceptos que aplican al canal según los filtros
        // Sistema unificado: todos los conceptos se obtienen desde conceptos_gastos
        // usando id_canal
        String nombreCanal = productoCanal.getCanal().getCanal();
        boolean esCanalNube = nombreCanal != null && (nombreCanal.equalsIgnoreCase("KT HOGAR")
                || nombreCanal.equalsIgnoreCase("KT GASTRO"));

        List<ConceptoGasto> conceptos = obtenerConceptosAplicables(
                idCanal,
                numeroCuotas,
                esCanalNube,
                producto);

        // Convertir a CanalConcepto para mantener compatibilidad con
        // calcularPrecioInterno
        List<CanalConcepto> conceptosCanal = convertirConceptosACanalConcepto(conceptos, idCanal);

        return calcularPrecioInterno(producto, productoCanal, conceptosCanal, numeroCuotas);
    }

    @Override
    @Transactional
    public PrecioCalculadoDTO recalcularYGuardarPrecioCanal(Integer idProducto, Integer idCanal, Integer numeroCuotas) {
        Producto producto = obtenerProducto(idProducto);
        ProductoCanal productoCanal = obtenerProductoCanal(idProducto, idCanal);

        // Obtener todos los conceptos que aplican al canal según los filtros
        // Sistema unificado: todos los conceptos se obtienen desde conceptos_gastos
        // usando id_canal
        String nombreCanal = productoCanal.getCanal().getCanal();
        boolean esCanalNube = nombreCanal != null && (nombreCanal.equalsIgnoreCase("KT HOGAR")
                || nombreCanal.equalsIgnoreCase("KT GASTRO"));

        List<ConceptoGasto> conceptos = obtenerConceptosAplicables(
                idCanal,
                numeroCuotas,
                esCanalNube,
                producto);

        // Convertir a CanalConcepto para mantener compatibilidad con
        // calcularPrecioInterno
        List<CanalConcepto> conceptosCanal = convertirConceptosACanalConcepto(conceptos, idCanal);

        PrecioCalculadoDTO dto = calcularPrecioInterno(producto, productoCanal, conceptosCanal, numeroCuotas);

        // Persistimos/actualizamos en producto_canal_precios
        ProductoCanalPrecio pcp = productoCanalPrecioRepository
                .findByProductoIdAndCanalId(idProducto, idCanal)
                .orElseGet(ProductoCanalPrecio::new);

        if (pcp.getId() == null) {
            pcp.setProducto(producto);
            pcp.setCanal(productoCanal.getCanal());
        }

        pcp.setPvp(dto.pvp());
        pcp.setCostoTotal(dto.costoTotal());
        pcp.setGananciaAbs(dto.gananciaAbs());
        pcp.setGananciaPorcentaje(dto.gananciaPorcentaje());
        pcp.setGastosTotalPorcentaje(dto.gastosTotalPorcentaje());

        productoCanalPrecioRepository.save(pcp);

        return dto;
    }

    // ====================================================
    // LÓGICA DE CÁLCULO
    // ====================================================

    /**
     * Calcula el precio de un producto para un canal basándose en la lógica del
     * Excel.
     * 
     * Fórmula principal del Excel para ML:
     * PVP ML = ((([@[ML-COSTO+GAN]] + [@ENVIO]) * [@[IMP.]]) / ([@[%CUOTAS]])) /
     * 0,9
     * 
     * Donde:
     * - ML-COSTO+GAN = [@COSTO] + ([@COSTO] * [@[GAN.MIN.ML]])
     * - @COSTO = costo del producto (de DUX, tabla productos.costo)
     * - @ENVIO = precio de envío de ML (de tabla mlas.precio_envio)
     * - @[GAN.MIN.ML] = % de ganancia mínima del producto (de
     * producto_canal.margen_porcentaje)
     * - @[IMP.] = 1 + [@IVA] + sum(conceptos con aplica_sobre='IMP')
     * - @IVA = IVA del producto (de DUX, tabla productos.iva, como porcentaje, ej:
     * 21)
     * - conceptos IMP = conceptos de conceptos_gastos con aplica_sobre='IMP' (ej:
     * IIBB = 5%)
     * - @[%CUOTAS] = (1 - BUSCARX([@CUOTAS]; GTML[CUOTAS]; GTML[%]))
     * - GTML[%] = suma de porcentajes de conceptos_gastos con campo cuotas = número
     * de cuotas
     * - Se obtiene de conceptos_gastos filtrando por id_canal=ML,
     * cuotas="3"/"6"/"9"/"12", etc.
     * - 0,9 = gasto fijo del 10% para ML (equivalente a dividir por 0.9)
     * 
     * Para otros canales, la fórmula es similar pero sin envío y sin el divisor
     * 0.9.
     */
    private PrecioCalculadoDTO calcularPrecioInterno(
            Producto producto,
            ProductoCanal productoCanal,
            List<CanalConcepto> conceptos,
            Integer numeroCuotas) {
        if (producto.getCosto() == null) {
            throw new BadRequestException("El producto no tiene costo cargado");
        }

        if (producto.getIva() == null) {
            throw new BadRequestException("El producto no tiene IVA cargado");
        }

        BigDecimal costo = producto.getCosto();
        BigDecimal iva = producto.getIva();
        BigDecimal cien = BigDecimal.valueOf(100);

        // 1) Separar conceptos que se suman al factor IMP (como IIBB)
        List<CanalConcepto> gastosSobreImp = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.IMP)
                .collect(Collectors.toList());

        // 2) Calcular factor de impuestos (IMP = 1 + IVA/100 + sum(conceptos IMP)/100)
        // Según fórmula Excel: @[IMP.] = 1 + [@IVA] + IIBB
        // Donde IVA viene de DUX (como porcentaje, ej: 21) e IIBB viene de
        // conceptos_gastos
        BigDecimal gastosSobreImpTotal = calcularGastosPorcentaje(gastosSobreImp);
        BigDecimal imp = BigDecimal.ONE
                .add(iva.divide(cien, 6, RoundingMode.HALF_UP))
                .add(gastosSobreImpTotal.divide(cien, 6, RoundingMode.HALF_UP));

        // 3) Obtener margen base del canal
        BigDecimal margenPorcentaje = productoCanal.getMargenPorcentaje();
        if (margenPorcentaje == null) {
            margenPorcentaje = BigDecimal.ZERO;
        }

        // 4) Aplicar márgenes adicionales si existen
        BigDecimal margenFijo = productoCanal.getMargenFijo() != null ? productoCanal.getMargenFijo() : BigDecimal.ZERO;
        BigDecimal margenPromocion = productoCanal.getMargenPromocion() != null ? productoCanal.getMargenPromocion()
                : BigDecimal.ZERO;
        BigDecimal margenOferta = productoCanal.getMargenOferta() != null ? productoCanal.getMargenOferta()
                : BigDecimal.ZERO;

        // 5) Separar conceptos de gasto según aplicaSobre (excluyendo IMP que ya se
        // procesó)
        List<CanalConcepto> gastosSobreCosto = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.COSTO)
                .collect(Collectors.toList());

        List<CanalConcepto> gastosSobreCostoMargen = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.COSTO_MARGEN)
                .collect(Collectors.toList());

        List<CanalConcepto> gastosSobreCostoIva = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.COSTO_IVA)
                .collect(Collectors.toList());

        List<CanalConcepto> gastosSobrePVP = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.PVP)
                .collect(Collectors.toList());

        // 5) Calcular gastos sobre COSTO (se suman al costo base)
        BigDecimal gastosSobreCostoTotal = calcularGastosPorcentaje(gastosSobreCosto);
        BigDecimal costoConGastos = costo.multiply(
                BigDecimal.ONE.add(gastosSobreCostoTotal.divide(cien, 6, RoundingMode.HALF_UP)));

        // 6) Calcular costo con ganancia (margen porcentual)
        // Según fórmula Excel: ML-COSTO+GAN = [@COSTO] + ([@COSTO] * [@[GAN.MIN.ML]])
        // Esto es equivalente a: COSTO * (1 + GAN.MIN.ML/100)
        BigDecimal margenFrac = margenPorcentaje.divide(cien, 6, RoundingMode.HALF_UP);
        BigDecimal costoConGanancia = costoConGastos.multiply(BigDecimal.ONE.add(margenFrac));

        // 7) Aplicar gastos sobre COSTO_MARGEN
        BigDecimal gastosSobreCostoMargenTotal = calcularGastosPorcentaje(gastosSobreCostoMargen);
        BigDecimal costoConGananciaFinal = costoConGanancia.multiply(
                BigDecimal.ONE.add(gastosSobreCostoMargenTotal.divide(cien, 6, RoundingMode.HALF_UP)));

        // 7.5) Sumar envío (solo para canal ML, antes de aplicar IVA)
        // Según fórmula Excel: ((COSTO+GAN)+ENVIO)*IMP
        BigDecimal costoConGananciaYEnvio = costoConGananciaFinal;
        String nombreCanal = productoCanal.getCanal().getCanal();
        if (nombreCanal != null && nombreCanal.toUpperCase().contains("ML")) {
            // Buscar el precio de envío del producto
            BigDecimal precioEnvio = mlaRepository.findByProductoId(producto.getId())
                    .stream()
                    .findFirst()
                    .map(mla -> mla.getPrecioEnvio())
                    .filter(envio -> envio != null && envio.compareTo(BigDecimal.ZERO) > 0)
                    .orElse(BigDecimal.ZERO);

            if (precioEnvio.compareTo(BigDecimal.ZERO) > 0) {
                costoConGananciaYEnvio = costoConGananciaFinal.add(precioEnvio);
            }
        }

        // 8) Aplicar impuestos (IVA)
        BigDecimal costoConImpuestos = costoConGananciaYEnvio.multiply(imp);

        // 9) Aplicar gastos sobre COSTO_IVA
        BigDecimal gastosSobreCostoIvaTotal = calcularGastosPorcentaje(gastosSobreCostoIva);
        costoConImpuestos = costoConImpuestos.multiply(
                BigDecimal.ONE.add(gastosSobreCostoIvaTotal.divide(cien, 6, RoundingMode.HALF_UP)));

        // 10) Calcular gastos totales sobre PVP (para el denominador)
        // NOTA: Si hay cuotas, los gastos sobre PVP que forman parte de GTML[%] NO se
        // aplican aquí,
        // sino solo en el cálculo de cuotas. Por lo tanto, si hay cuotas, no aplicamos
        // gastos sobre PVP aquí.
        BigDecimal gastosSobrePVPTotal = BigDecimal.ZERO;
        BigDecimal gastosSobrePVPFrac = BigDecimal.ZERO;

        // Solo aplicar gastos sobre PVP si NO hay cuotas (pago contado)
        // Si hay cuotas, estos gastos se aplican en el cálculo de cuotas como parte de
        // GTML[%]
        if (productoCanal.getAplicaCuotas() == null || !productoCanal.getAplicaCuotas()
                || numeroCuotas == null) {
            gastosSobrePVPTotal = calcularGastosPorcentaje(gastosSobrePVP);
            gastosSobrePVPFrac = gastosSobrePVPTotal.divide(cien, 6, RoundingMode.HALF_UP);
        }

        // 11) Calcular PVP base: PVP = costoConImpuestos / (1 - gastosSobrePVP)
        // Si hay cuotas, gastosSobrePVPFrac será 0, por lo que pvpBase =
        // costoConImpuestos
        BigDecimal denominador = BigDecimal.ONE.subtract(gastosSobrePVPFrac);
        if (denominador.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Los gastos sobre PVP son >= 100%, lo cual es inválido");
        }

        BigDecimal pvpBase = costoConImpuestos.divide(denominador, 6, RoundingMode.HALF_UP);

        // 11.5) Aplicar porcentaje de cuotas (si aplicaCuotas es true y se especifica
        // número de cuotas)
        // Según fórmula Excel: ... / %CUOTAS
        // Donde %CUOTAS = (1 - BUSCARX([@CUOTAS]; GTML[CUOTAS]; GTML[%]))
        // GTML[%] = suma de porcentajes de conceptos específicos del canal + porcentaje
        // de
        // canal_concepto_cuota
        // Para ML: COMISION ML + %CUOTAS + MARKETING + EMBALAJE
        // Para NUBE: MP + %CUOTAS + MARKETING + (NUBE + EMBALAJE si no es máquina)
        // Estos conceptos deben tener aplica_sobre='PVP' y estar en canal_concepto
        // Si numeroCuotas es null, no se aplican gastos de cuotas (pago contado)
        if (productoCanal.getAplicaCuotas() != null && productoCanal.getAplicaCuotas()
                && numeroCuotas != null) {
            Integer idCanal = productoCanal.getCanal().getId();

            // 1) Obtener todos los conceptos del canal con aplica_sobre='PVP' que forman
            // parte de GTML[%]
            // Estos son los conceptos específicos que deben incluirse en el cálculo de
            // cuotas
            List<CanalConcepto> todosConceptosCanal = canalConceptoRepository.findByCanalId(idCanal);

            // También incluir conceptos del canal padre si existe (jerarquía)
            Canal canalActual = canalRepository.findById(idCanal).orElse(null);
            if (canalActual != null && canalActual.getCanalBase() != null) {
                todosConceptosCanal.addAll(canalConceptoRepository.findByCanalId(canalActual.getCanalBase().getId()));
            }

            // Filtrar solo conceptos con aplica_sobre='PVP' que forman parte de GTML[%]
            BigDecimal porcentajeConceptosCanal = todosConceptosCanal.stream()
                    .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.PVP)
                    .map(cc -> cc.getConcepto().getPorcentaje())
                    .filter(p -> p != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 2) Obtener el porcentaje de cuotas de canal_concepto_cuota (si existe)
            // Buscar cuotas NORMAL primero, luego PROMO si no hay NORMAL
            BigDecimal porcentajeCuota = BigDecimal.ZERO;
            List<CanalConceptoCuota> cuotasCanal = canalConceptoCuotaRepository.findByCanalIdAndCuotas(idCanal,
                    numeroCuotas);

            // También buscar en el canal padre si existe (jerarquía)
            if (canalActual != null && canalActual.getCanalBase() != null) {
                cuotasCanal.addAll(canalConceptoCuotaRepository
                        .findByCanalIdAndCuotas(canalActual.getCanalBase().getId(), numeroCuotas));
            }

            // Prioridad: NORMAL primero, luego PROMO
            Optional<CanalConceptoCuota> cuotaNormal = cuotasCanal.stream()
                    .filter(c -> c.getTipo() == TipoCuota.NORMAL)
                    .findFirst();

            if (cuotaNormal.isPresent()) {
                porcentajeCuota = cuotaNormal.get().getPorcentaje();
            } else {
                Optional<CanalConceptoCuota> cuotaPromo = cuotasCanal.stream()
                        .filter(c -> c.getTipo() == TipoCuota.PROMO)
                        .findFirst();
                if (cuotaPromo.isPresent()) {
                    porcentajeCuota = cuotaPromo.get().getPorcentaje();
                }
            }

            // 3) GTML[%] = suma de conceptos con aplica_sobre='PVP' + porcentaje de cuotas
            BigDecimal porcentajeCuotasTotal = porcentajeConceptosCanal.add(porcentajeCuota);

            if (porcentajeCuotasTotal.compareTo(BigDecimal.ZERO) > 0) {
                // El porcentaje de cuotas se aplica como divisor según Excel: / (1 -
                // %CUOTAS/100)
                // Ejemplo: si %CUOTAS = 5%, dividimos por (1 - 0.05) = 0.95
                BigDecimal cuotasFrac = porcentajeCuotasTotal.divide(cien, 6, RoundingMode.HALF_UP);
                BigDecimal divisorCuotas = BigDecimal.ONE.subtract(cuotasFrac);
                if (divisorCuotas.compareTo(BigDecimal.ZERO) > 0) {
                    pvpBase = pvpBase.divide(divisorCuotas, 6, RoundingMode.HALF_UP);
                }
            }
        }

        // 12) Aplicar reglas de descuento (según fórmula: PVP = ... / (1 - descuentos))
        BigDecimal descuentoTotal = obtenerDescuentoAplicable(producto, productoCanal.getCanal().getId(), pvpBase);
        BigDecimal pvp = pvpBase;
        if (descuentoTotal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal descuentoFrac = descuentoTotal.divide(cien, 6, RoundingMode.HALF_UP);
            BigDecimal denominadorDescuento = BigDecimal.ONE.subtract(descuentoFrac);
            if (denominadorDescuento.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("El descuento es >= 100%, lo cual es inválido");
            }
            pvp = pvp.divide(denominadorDescuento, 6, RoundingMode.HALF_UP);
        }

        // 12.5) Aplicar gasto fijo según configuración del canal
        // El porcentaje se obtiene del campo porcentaje_retencion de la tabla canales
        // Por defecto es 0 (no afecta el cálculo)
        // Para ML típicamente es 10 (10% de gasto fijo, equivalente a dividir por 0.9)
        // Fórmula: divisor = 1 - (porcentaje / 100)
        Canal canal = productoCanal.getCanal();
        if (canal != null && canal.getPorcentajeRetencion() != null) {
            BigDecimal porcentaje = canal.getPorcentajeRetencion();
            if (porcentaje.compareTo(BigDecimal.ZERO) > 0) {
                // Convertir porcentaje a divisor: divisor = 1 - (porcentaje / 100)
                BigDecimal divisor = BigDecimal.ONE.subtract(porcentaje.divide(cien, 6, RoundingMode.HALF_UP));
                if (divisor.compareTo(BigDecimal.ZERO) > 0) {
                    pvp = pvp.divide(divisor, 6, RoundingMode.HALF_UP);
                }
            }
        }

        // 13) Aplicar márgenes adicionales (fijo, promoción, oferta)
        // Estos se aplican como incrementos sobre el PVP después de descuentos
        if (margenFijo.compareTo(BigDecimal.ZERO) > 0) {
            pvp = pvp.add(margenFijo);
        }
        if (margenPromocion.compareTo(BigDecimal.ZERO) > 0) {
            pvp = pvp.multiply(BigDecimal.ONE.add(margenPromocion.divide(cien, 6, RoundingMode.HALF_UP)));
        }
        if (margenOferta.compareTo(BigDecimal.ZERO) > 0) {
            pvp = pvp.multiply(BigDecimal.ONE.add(margenOferta.divide(cien, 6, RoundingMode.HALF_UP)));
        }

        // Redondear PVP final
        pvp = pvp.setScale(2, RoundingMode.HALF_UP);

        // 14) Calcular costo total (costo base + gastos sobre costo)
        BigDecimal costoTotal = costoConGastos.setScale(2, RoundingMode.HALF_UP);

        // 15) Calcular ganancia absoluta y porcentaje
        BigDecimal gananciaAbs = pvp.subtract(costoConImpuestos).setScale(2, RoundingMode.HALF_UP);

        BigDecimal gananciaPorcentaje = BigDecimal.ZERO;
        if (costoConImpuestos.compareTo(BigDecimal.ZERO) > 0) {
            gananciaPorcentaje = gananciaAbs
                    .multiply(cien)
                    .divide(costoConImpuestos, 2, RoundingMode.HALF_UP);
        }

        // 16) Calcular gastos totales porcentaje (suma de todos los gastos, incluyendo
        // IMP)
        BigDecimal gastosTotalPorcentaje = gastosSobreCostoTotal
                .add(gastosSobreCostoMargenTotal)
                .add(gastosSobreCostoIvaTotal)
                .add(gastosSobrePVPTotal)
                .add(gastosSobreImpTotal);

        return new PrecioCalculadoDTO(
                pvp,
                costoTotal,
                gananciaAbs,
                gananciaPorcentaje,
                gastosTotalPorcentaje);
    }

    /**
     * Suma el porcentaje total de gastos de una lista de conceptos.
     */
    private BigDecimal calcularGastosPorcentaje(List<CanalConcepto> conceptos) {
        return conceptos.stream()
                .map(cc -> cc.getConcepto().getPorcentaje())
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Obtiene el descuento aplicable al producto según las reglas de descuento del
     * canal.
     * Retorna el descuento porcentual de la regla de mayor prioridad que cumpla
     * todas las condiciones.
     * 
     * @param producto El producto al que se aplicará el descuento
     * @param canalId  El ID del canal
     * @param pvpBase  El PVP base antes de aplicar descuentos (para validar monto
     *                 mínimo)
     * @return El porcentaje de descuento aplicable (0 si no hay descuento
     *         aplicable)
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

            // Verificar catálogo (si la regla tiene catálogo, el producto debe estar en ese
            // catálogo)
            if (regla.getCatalogo() != null) {
                if (!catalogosProducto.contains(regla.getCatalogo().getId())) {
                    continue;
                }
            }

            // Verificar clasificación general (si la regla tiene clasifGral, debe
            // coincidir)
            if (regla.getClasifGral() != null) {
                if (producto.getClasifGral() == null ||
                        !producto.getClasifGral().getId().equals(regla.getClasifGral().getId())) {
                    continue;
                }
            }

            // Verificar clasificación gastro (si la regla tiene clasifGastro, debe
            // coincidir)
            if (regla.getClasifGastro() != null) {
                if (producto.getClasifGastro() == null ||
                        !producto.getClasifGastro().getId().equals(regla.getClasifGastro().getId())) {
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
        return productoRepository.findById(idProducto)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado"));
    }

    private ProductoCanal obtenerProductoCanal(Integer idProducto, Integer idCanal) {
        return productoCanalRepository
                .findByProductoIdAndCanalId(idProducto, idCanal)
                .orElseThrow(() -> new NotFoundException("No existe configuración de canal para este producto"));
    }

    /**
     * Obtiene todos los conceptos de gasto que aplican al canal según los filtros.
     * 
     * Sistema unificado: todos los conceptos se asocian a canales a través de
     * canal_concepto.
     * 
     * Lógica de filtrado:
     * - Canal: Un concepto aplica a un canal si está asociado en canal_concepto
     * - Jerarquía de canales: Si un concepto está asignado al canal padre (ej:
     * NUBE),
     * también aplica a todos sus canales hijos (ej: KT HOGAR, KT GASTRO)
     * Nota: Las cuotas ahora se manejan a través de canal_concepto_cuota, no a
     * través
     * del campo cuotas en conceptos_gastos
     * 
     * REGLAS DE CANAL_CONCEPTO_REGLA:
     * - Si tipo_regla = INCLUIR: el concepto SOLO aplica si el producto cumple
     * TODAS las condiciones
     * - Si tipo_regla = EXCLUIR: el concepto NO aplica si el producto cumple ALGUNA
     * condición
     * 
     * @param idCanal      ID del canal para filtrar conceptos
     * @param numeroCuotas Número de cuotas (parámetro mantenido por compatibilidad,
     *                     pero ya no se usa para filtrar conceptos)
     * @param esCanalNube  true si el canal es KT HOGAR o KT GASTRO (canales NUBE)
     * @param producto     El producto para aplicar las reglas de
     *                     canal_concepto_regla
     * @return Lista de conceptos de gasto que aplican según los filtros
     */
    private List<ConceptoGasto> obtenerConceptosAplicables(Integer idCanal, Integer numeroCuotas,
            boolean esCanalNube, Producto producto) {
        // Obtener el canal actual para acceder a su canal padre (canalBase)
        Canal canalActual = canalRepository.findById(idCanal).orElse(null);
        final Integer idCanalPadre = (canalActual != null && canalActual.getCanalBase() != null)
                ? canalActual.getCanalBase().getId()
                : null;

        // Obtener conceptos asociados al canal a través de canal_concepto
        List<CanalConcepto> conceptosPorCanal = canalConceptoRepository.findByCanalId(idCanal);
        // También incluir conceptos del canal padre si existe (jerarquía)
        if (idCanalPadre != null) {
            conceptosPorCanal.addAll(canalConceptoRepository.findByCanalId(idCanalPadre));
        }

        // Obtener todas las reglas del canal (incluyendo el canal padre si existe)
        List<CanalConceptoRegla> reglasCanal = canalConceptoReglaRepository.findByCanalId(idCanal);
        if (idCanalPadre != null) {
            reglasCanal.addAll(canalConceptoReglaRepository.findByCanalId(idCanalPadre));
        }

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
     * @return true si el producto cumple TODAS las condiciones especificadas en la
     *         regla
     */
    private boolean cumpleCondicionesRegla(CanalConceptoRegla regla, Producto producto) {
        // Si la regla no tiene condiciones, no se aplica (retorna false)
        boolean tieneCondiciones = regla.getTipo() != null
                || regla.getClasifGral() != null
                || regla.getClasifGastro() != null
                || regla.getMarca() != null;

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

        // Si llegamos aquí, el producto cumple TODAS las condiciones especificadas
        return true;
    }

    /**
     * Convierte una lista de ConceptoGasto a CanalConcepto para mantener
     * compatibilidad
     * con el método calcularPrecioInterno que espera List<CanalConcepto>.
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

}