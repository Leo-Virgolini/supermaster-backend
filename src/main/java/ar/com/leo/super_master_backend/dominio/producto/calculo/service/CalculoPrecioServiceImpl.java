package ar.com.leo.super_master_backend.dominio.producto.calculo.service;

import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConcepto;
import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConceptoCuota;
import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConceptoRegla;
import ar.com.leo.super_master_backend.dominio.canal.entity.TipoCuota;
import ar.com.leo.super_master_backend.dominio.canal.entity.TipoRegla;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.entity.AplicaSobre;
import ar.com.leo.super_master_backend.dominio.producto.calculo.dto.PrecioCalculadoDTO;
import ar.com.leo.super_master_backend.dominio.producto.calculo.dto.FormulaCalculoDTO;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanal;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanalPrecio;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanalPromocion;
import ar.com.leo.super_master_backend.dominio.promocion.entity.Promocion;
import ar.com.leo.super_master_backend.dominio.promocion.entity.TipoPromocionTabla;
import ar.com.leo.super_master_backend.dominio.canal.entity.Canal;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalConceptoCuotaRepository;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalConceptoReglaRepository;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalConceptoRepository;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalRepository;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.entity.ConceptoGasto;
import ar.com.leo.super_master_backend.dominio.producto.mla.repository.MlaRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalPrecioRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalPromocionRepository;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalculoPrecioServiceImpl implements CalculoPrecioService {

    private final ProductoRepository productoRepository;
    private final ProductoCanalRepository productoCanalRepository;
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
        // Detectar si es KT HOGAR o KT GASTRO y usar fórmula especial
        String nombreCanal = productoCanal.getCanal().getCanal();
        if (nombreCanal != null && nombreCanal.equalsIgnoreCase("KT HOGAR")) {
            return calcularPrecioKTHogar(producto, productoCanal, conceptos, numeroCuotas);
        }
        if (nombreCanal != null && nombreCanal.equalsIgnoreCase("KT GASTRO")) {
            return calcularPrecioKTGastro(producto, productoCanal, conceptos, numeroCuotas);
        }

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

        // 13) Aplicar márgenes adicionales (fijo)
        // Estos se aplican como incrementos sobre el PVP después de descuentos
        if (margenFijo.compareTo(BigDecimal.ZERO) > 0) {
            pvp = pvp.add(margenFijo);
        }

        // Redondear PVP sin promociones
        BigDecimal pvpSinPromocion = pvp.setScale(2, RoundingMode.HALF_UP);

        // 13.5) Aplicar promociones (incluye porcentaje_inflacion de canales y producto_canal_promocion)
        BigDecimal pvpInflado = aplicarPromocion(producto.getId(), productoCanal.getCanal(), pvpSinPromocion);
        pvpInflado = pvpInflado.setScale(2, RoundingMode.HALF_UP);

        // 14) Calcular costo total (costo base + gastos sobre costo)
        BigDecimal costoTotal = costoConGastos.setScale(2, RoundingMode.HALF_UP);

        // 15) Calcular ganancia absoluta y porcentaje (usando PVP sin promociones)
        BigDecimal gananciaAbs = pvpSinPromocion.subtract(costoConImpuestos).setScale(2, RoundingMode.HALF_UP);

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
                pvpSinPromocion,
                pvpInflado,
                costoTotal,
                gananciaAbs,
                gananciaPorcentaje,
                gastosTotalPorcentaje);
    }

    /**
     * Determina si un producto es máquina basándose en la clasificación gastronómica.
     * Un producto es máquina si su clasifGastro tiene es_maquina = true.
     * 
     * @param producto El producto a verificar
     * @return true si el producto es máquina, false en caso contrario
     */
    private boolean esProductoMaquina(Producto producto) {
        if (producto == null || producto.getClasifGastro() == null) {
            return false;
        }
        Boolean esMaquina = producto.getClasifGastro().getEsMaquina();
        return esMaquina != null && esMaquina;
    }

    /**
     * Calcula el precio para el canal KT HOGAR usando la fórmula especial del Excel.
     * 
     * Fórmula: PVP NUBE = ( ( ([@COSTO]+[@COSTO]*( SI( [@[GAN.MIN.ML]]=0; "MARGEN"; [@[GAN.MIN.ML]] + 0,25 ) )) * [@[IMP.]] ) / (1-GTN) ) / (1-CUPON)
     * 
     * Donde:
     * - Si GAN.MIN.ML = 0, usar MARGEN (margen base del canal)
     * - Si GAN.MIN.ML != 0, usar GAN.MIN.ML + 0.25
     * - @[IMP.] = 1 + [@IVA] + IIBB
     * - GTN (NUBE MENAJE X CUOTAS) = MP + NUBE + X CUOTAS + MARKETING + EMBALAJE
     *   (donde X es el número de cuotas especificado)
     * - CUPON se obtiene de conceptos_gastos (si existe)
     * 
     * NOTA: NO SE PUEDE VENDER MAQUINA en este canal
     */
    private PrecioCalculadoDTO calcularPrecioKTHogar(
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

        // 1) Calcular factor de impuestos (IMP = 1 + IVA/100 + IIBB/100)
        List<CanalConcepto> gastosSobreImp = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.IMP)
                .collect(Collectors.toList());

        BigDecimal gastosSobreImpTotal = calcularGastosPorcentaje(gastosSobreImp);
        BigDecimal imp = BigDecimal.ONE
                .add(iva.divide(cien, 6, RoundingMode.HALF_UP))
                .add(gastosSobreImpTotal.divide(cien, 6, RoundingMode.HALF_UP));

        // 2) Calcular ganancia según fórmula:
        // SI( [@[GAN.MIN.ML]]=0; "MARGEN"; [@[GAN.MIN.ML]] + COSTO_MARGEN )
        // El COSTO_MARGEN viene de un concepto de gasto con aplica_sobre='COSTO_MARGEN' asociado al canal
        // El valor en conceptos_gastos se guarda como porcentaje (ej: 25 = 25%)
        // Se suma directamente al margen sin dividir por 100
        // NOTA: Los conceptos ya vienen filtrados por reglas de canal_concepto_regla desde obtenerConceptosAplicables
        BigDecimal margenPorcentaje = productoCanal.getMargenPorcentaje();
        if (margenPorcentaje == null) {
            margenPorcentaje = BigDecimal.ZERO;
        }

        // Filtrar solo los conceptos con aplica_sobre='COSTO_MARGEN' de los conceptos ya filtrados por reglas
        List<CanalConcepto> gastosSobreCostoMargen = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.COSTO_MARGEN)
                .collect(Collectors.toList());

        BigDecimal conceptoCostoMargen = calcularGastosPorcentaje(gastosSobreCostoMargen);

        BigDecimal gananciaUsar;
        if (margenPorcentaje.compareTo(BigDecimal.ZERO) == 0) {
            // Si GAN.MIN.ML = 0, usar MARGEN (margen base del canal)
            // Por ahora, usamos 0 si no hay margen (esto debería revisarse según la lógica de negocio)
            gananciaUsar = BigDecimal.ZERO;
        } else {
            // Si GAN.MIN.ML != 0, usar GAN.MIN.ML + conceptoCostoMargen
            // Ejemplo: si margen = 60% y COSTO_MARGEN = 25%, entonces ganancia = 60% + 25% = 85%
            gananciaUsar = margenPorcentaje.add(conceptoCostoMargen);
        }

        // 3) Calcular costo con ganancia: [@COSTO] + [@COSTO] * ganancia
        BigDecimal gananciaFrac = gananciaUsar.divide(cien, 6, RoundingMode.HALF_UP);
        BigDecimal costoConGanancia = costo.multiply(BigDecimal.ONE.add(gananciaFrac));

        // 4) Aplicar impuestos: (COSTO + COSTO * ganancia) * IMP
        BigDecimal costoConImpuestos = costoConGanancia.multiply(imp);

        // 5) Calcular GTN = MP + NUBE + X CUOTAS + MARKETING + EMBALAJE
        // GTN es la suma de todos los conceptos con aplica_sobre='PVP' del canal
        // más el porcentaje de cuotas
        // NOTA: Los conceptos ya vienen filtrados por reglas de canal_concepto_regla desde obtenerConceptosAplicables

        // Sumar porcentajes de conceptos PVP (MP + NUBE + MARKETING + EMBALAJE) de los conceptos ya filtrados
        BigDecimal porcentajeConceptosPVP = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.PVP)
                .map(cc -> cc.getConcepto().getPorcentaje())
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Obtener porcentaje de cuotas (usando el número de cuotas especificado)
        BigDecimal porcentajeCuota = BigDecimal.ZERO;
        // Si no se especifica número de cuotas, no se aplica porcentaje de cuotas
        if (numeroCuotas != null && numeroCuotas > 0) {
            Integer idCanal = productoCanal.getCanal().getId();
            List<CanalConceptoCuota> cuotasCanal = canalConceptoCuotaRepository.findByCanalIdAndCuotas(idCanal,
                    numeroCuotas);

            // También buscar en el canal padre si existe (jerarquía)
            Canal canalActual = canalRepository.findById(idCanal).orElse(null);
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
        }

        // GTN = MP + NUBE + X CUOTAS + MARKETING + EMBALAJE
        // (donde X es el número de cuotas especificado, o 0 si no hay cuotas)
        BigDecimal gtn = porcentajeConceptosPVP.add(porcentajeCuota);

        // 6) Calcular PVP: (costoConImpuestos) / (1 - GTN) / (1 - CUPON)
        // Para evitar acumulación de errores de redondeo en divisiones consecutivas,
        // combinamos las dos divisiones en una sola operación: costoConImpuestos / ((1-GTN) * (1-CUPON))
        BigDecimal gtnFrac = gtn.divide(cien, 10, RoundingMode.HALF_UP);
        BigDecimal denominadorGTN = BigDecimal.ONE.subtract(gtnFrac);

        if (denominadorGTN.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("GTN es >= 100%, lo cual es inválido");
        }

        // Obtener CUPON
        List<CanalConcepto> conceptosCupon = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.CUPON)
                .collect(Collectors.toList());

        BigDecimal porcentajeCupon = calcularGastosPorcentaje(conceptosCupon);

        // Calcular denominador combinado: (1 - GTN) * (1 - CUPON)
        BigDecimal denominadorCombinado = denominadorGTN;
        if (porcentajeCupon.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal cuponFrac = porcentajeCupon.divide(cien, 10, RoundingMode.HALF_UP);
            BigDecimal denominadorCupon = BigDecimal.ONE.subtract(cuponFrac);
            if (denominadorCupon.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("CUPON es >= 100%, lo cual es inválido");
            }
            // Combinar ambos denominadores: (1 - GTN) * (1 - CUPON)
            denominadorCombinado = denominadorGTN.multiply(denominadorCupon);
        }

        // Una sola división en lugar de dos consecutivas para mayor precisión
        BigDecimal pvp = costoConImpuestos.divide(denominadorCombinado, 10, RoundingMode.HALF_UP);

        // Redondear PVP sin promociones (para mostrar)
        BigDecimal pvpSinPromocion = pvp.setScale(2, RoundingMode.HALF_UP);

        // 6.6) Aplicar promociones (incluye porcentaje_inflacion de canales y producto_canal_promocion)
        BigDecimal pvpInflado = aplicarPromocion(producto.getId(), productoCanal.getCanal(), pvpSinPromocion);
        pvpInflado = pvpInflado.setScale(2, RoundingMode.HALF_UP);

        // 7) Calcular costo total (costo base, sin gastos adicionales en esta fórmula)
        BigDecimal costoTotal = costo.setScale(2, RoundingMode.HALF_UP);

        // 8) Calcular ganancia absoluta y porcentaje según fórmula Excel:
        // ( ( ([@[PVP NUBE]]*(1-CUPON)) * (1-GTN)) / ([@[IMP.]]) - [@COSTO] ) / [@COSTO]
        // Donde GTN = suma de conceptos gastos + %cuotas
        // NOTA: La ganancia es constante independientemente de las cuotas porque revertimos GTN correctamente
        BigDecimal gananciaAbs = BigDecimal.ZERO;
        BigDecimal gananciaPorcentaje = BigDecimal.ZERO;

        if (costo.compareTo(BigDecimal.ZERO) > 0 && imp.compareTo(BigDecimal.ZERO) > 0) {
            // Revertir CUPON: PVP * (1 - CUPON/100)
            BigDecimal pvpSinCupon = pvp;
            if (porcentajeCupon.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal cuponFrac = porcentajeCupon.divide(cien, 10, RoundingMode.HALF_UP);
                BigDecimal denominadorCupon = BigDecimal.ONE.subtract(cuponFrac);
                if (denominadorCupon.compareTo(BigDecimal.ZERO) > 0) {
                    pvpSinCupon = pvp.multiply(denominadorCupon);
                }
            }

            // Revertir GTN: * (1 - GTN/100)
            BigDecimal gtnFracRev = gtn.divide(cien, 10, RoundingMode.HALF_UP);
            BigDecimal denominadorGTNRev = BigDecimal.ONE.subtract(gtnFracRev);
            BigDecimal pvpSinCuponNiGTN = pvpSinCupon.multiply(denominadorGTNRev);

            // Revertir IMP: / IMP
            BigDecimal costoConGananciaCalculado = pvpSinCuponNiGTN.divide(imp, 10, RoundingMode.HALF_UP);

            // Calcular ganancia: costoConGanancia - COSTO
            gananciaAbs = costoConGananciaCalculado.subtract(costo).setScale(2, RoundingMode.HALF_UP);

            // Calcular porcentaje: gananciaAbs / COSTO * 100
            gananciaPorcentaje = gananciaAbs
                    .multiply(cien)
                    .divide(costo, 2, RoundingMode.HALF_UP);
        }

        // 9) Calcular gastos totales porcentaje (suma de todos los gastos, incluyendo CUPON)
        BigDecimal gastosTotalPorcentaje = gastosSobreImpTotal.add(gtn).add(porcentajeCupon);

        return new PrecioCalculadoDTO(
                pvpSinPromocion,
                pvpInflado,
                costoTotal,
                gananciaAbs,
                gananciaPorcentaje,
                gastosTotalPorcentaje);
    }

    /**
     * Calcula el precio para el canal KT GASTRO usando la fórmula especial del Excel.
     * 
     * Fórmula: PVP GASTRO = SI( [@TAG]="MAQUINA"; 
     *     ( [@[PVP ML]] * (1-0,145) ); 
     *     ( ( [@COSTO] * (1+([@[GAN.MIN.ML]] * (1-REL_ML_KTG))) * [@[IMP.]] ) / (1-(MARKETING+EMBALAJE+GASTONUBE)) ) )
     * 
     * Donde:
     * - Si es máquina: PVP GASTRO = PVP ML * (1 - 0.145)
     * - Si NO es máquina: PVP GASTRO = (COSTO * (1 + (GAN.MIN.ML * (1 - REL_ML_KTG))) * IMP) / (1 - (MARKETING + EMBALAJE + GASTONUBE))
     * - [@TAG]="MAQUINA" se determina por clasif_gastro.es_maquina = true
     * - [@[PVP ML]] = precio calculado para el canal ML
     * - [@[GAN.MIN.ML]] = margen_porcentaje del producto_canal para ML
     * - REL_ML_KTG = relación entre ML y KT GASTRO (valor a determinar)
     * - MARKETING, EMBALAJE, GASTONUBE = conceptos con aplica_sobre='PVP' del canal KT GASTRO
     */
    private PrecioCalculadoDTO calcularPrecioKTGastro(
            Producto producto,
            ProductoCanal productoCanal,
            List<CanalConcepto> conceptos,
            Integer numeroCuotas) {
        // Verificar si es máquina
        boolean esMaquina = esProductoMaquina(producto);

        if (esMaquina) {
            // Si es máquina: PVP GASTRO = PVP ML * (1 - 0.145)
            return calcularPrecioKTGastroMaquina(producto, productoCanal, numeroCuotas);
        } else {
            // Si NO es máquina: usar fórmula completa
            return calcularPrecioKTGastroNoMaquina(producto, productoCanal, conceptos, numeroCuotas);
        }
    }

    /**
     * Calcula el precio para KT GASTRO cuando el producto es máquina.
     * Fórmula: PVP GASTRO = PVP ML * (1 - 0.145)
     */
    private PrecioCalculadoDTO calcularPrecioKTGastroMaquina(
            Producto producto,
            ProductoCanal productoCanalKTGastro,
            Integer numeroCuotas) {
        // Buscar el canal ML
        Canal canalML = canalRepository.findByCanalIgnoreCase("ML")
                .orElseThrow(() -> new NotFoundException("No se encontró el canal ML"));

        // Obtener ProductoCanal para ML
        ProductoCanal productoCanalML = productoCanalRepository
                .findByProductoIdAndCanalId(producto.getId(), canalML.getId())
                .orElseThrow(() -> new NotFoundException(
                        "No se encontró la configuración del producto para el canal ML"));

        // Obtener conceptos para ML
        boolean esCanalNube = false; // ML no es canal nube
        List<ConceptoGasto> conceptosML = obtenerConceptosAplicables(
                canalML.getId(),
                numeroCuotas,
                esCanalNube,
                producto);

        // Convertir a CanalConcepto
        List<CanalConcepto> conceptosCanalML = convertirConceptosACanalConcepto(conceptosML, canalML.getId());

        // Calcular PVP ML
        PrecioCalculadoDTO precioML = calcularPrecioInterno(producto, productoCanalML, conceptosCanalML, numeroCuotas);

        // Aplicar descuento del 14.5%: PVP GASTRO = PVP ML * (1 - 0.145)
        BigDecimal factorDescuento = BigDecimal.valueOf(0.855); // 1 - 0.145
        BigDecimal pvpSinPromocion = precioML.pvp().multiply(factorDescuento).setScale(2, RoundingMode.HALF_UP);

        // Aplicar promociones del canal KT GASTRO (no ML)
        BigDecimal pvpInflado = aplicarPromocion(producto.getId(), productoCanalKTGastro.getCanal(), pvpSinPromocion);
        pvpInflado = pvpInflado.setScale(2, RoundingMode.HALF_UP);

        // Calcular ganancia desde el PVP ML original
        BigDecimal gananciaAbs = precioML.gananciaAbs();
        BigDecimal gananciaPorcentaje = precioML.gananciaPorcentaje();

        // Gastos totales (usar los de ML ya que se basa en ese precio)
        BigDecimal gastosTotalPorcentaje = precioML.gastosTotalPorcentaje();

        return new PrecioCalculadoDTO(
                pvpSinPromocion,
                pvpInflado,
                precioML.costoTotal(),
                gananciaAbs,
                gananciaPorcentaje,
                gastosTotalPorcentaje);
    }

    /**
     * Calcula el precio para KT GASTRO cuando el producto NO es máquina.
     * Fórmula: PVP GASTRO = (COSTO * (1 + (GAN.MIN.ML * (1 - REL_ML_KTG))) * IMP) / (1 - (MARKETING + EMBALAJE + GASTONUBE))
     */
    private PrecioCalculadoDTO calcularPrecioKTGastroNoMaquina(
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

        // 1) Calcular factor de impuestos (IMP = 1 + IVA/100 + sum(conceptos IMP)/100)
        List<CanalConcepto> gastosSobreImp = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.IMP)
                .collect(Collectors.toList());

        BigDecimal gastosSobreImpTotal = calcularGastosPorcentaje(gastosSobreImp);
        BigDecimal imp = BigDecimal.ONE
                .add(iva.divide(cien, 6, RoundingMode.HALF_UP))
                .add(gastosSobreImpTotal.divide(cien, 6, RoundingMode.HALF_UP));

        // 2) Obtener GAN.MIN.ML del canal ML
        Canal canalML = canalRepository.findByCanalIgnoreCase("ML")
                .orElseThrow(() -> new NotFoundException("No se encontró el canal ML"));

        ProductoCanal productoCanalML = productoCanalRepository
                .findByProductoIdAndCanalId(producto.getId(), canalML.getId())
                .orElse(null);

        BigDecimal gananciaMinML = BigDecimal.ZERO;
        if (productoCanalML != null && productoCanalML.getMargenPorcentaje() != null) {
            gananciaMinML = productoCanalML.getMargenPorcentaje();
        }

        // 3) Obtener REL_ML_KTG desde conceptos_gastos
        // Buscar conceptos con aplica_sobre='COSTO_MARGEN' asociados al canal KT GASTRO
        // REL_ML_KTG se usa para ajustar la ganancia mínima de ML: GAN.MIN.ML * (1 - REL_ML_KTG)
        List<CanalConcepto> conceptosRelMLKTG = conceptos.stream()
                .filter(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.COSTO_MARGEN)
                .collect(Collectors.toList());

        BigDecimal relMLKTG = calcularGastosPorcentaje(conceptosRelMLKTG);

        // 4) Calcular: COSTO * (1 + (GAN.MIN.ML * (1 - REL_ML_KTG)))
        BigDecimal factorRelacion = BigDecimal.ONE.subtract(relMLKTG.divide(cien, 6, RoundingMode.HALF_UP));
        BigDecimal gananciaAjustada = gananciaMinML.multiply(factorRelacion);
        BigDecimal gananciaFrac = gananciaAjustada.divide(cien, 6, RoundingMode.HALF_UP);
        BigDecimal costoConGanancia = costo.multiply(BigDecimal.ONE.add(gananciaFrac));

        // 5) Aplicar impuestos: * IMP
        BigDecimal costoConImpuestos = costoConGanancia.multiply(imp);

        // 6) Obtener conceptos MARKETING, EMBALAJE, GASTONUBE (aplica_sobre='PVP')
        List<CanalConcepto> gastosSobrePVP = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.PVP)
                .collect(Collectors.toList());

        BigDecimal gastosSobrePVPTotal = calcularGastosPorcentaje(gastosSobrePVP);
        BigDecimal gastosSobrePVPFrac = gastosSobrePVPTotal.divide(cien, 6, RoundingMode.HALF_UP);

        // 7) Calcular PVP: costoConImpuestos / (1 - (MARKETING + EMBALAJE + GASTONUBE))
        BigDecimal denominador = BigDecimal.ONE.subtract(gastosSobrePVPFrac);
        if (denominador.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Los gastos sobre PVP son >= 100%, lo cual es inválido");
        }

        BigDecimal pvp = costoConImpuestos.divide(denominador, 6, RoundingMode.HALF_UP);
        BigDecimal pvpSinPromocion = pvp.setScale(2, RoundingMode.HALF_UP);

        // 8) Aplicar promociones
        BigDecimal pvpInflado = aplicarPromocion(producto.getId(), productoCanal.getCanal(), pvpSinPromocion);
        pvpInflado = pvpInflado.setScale(2, RoundingMode.HALF_UP);

        // 9) Calcular ganancia
        BigDecimal costoTotal = costo.setScale(2, RoundingMode.HALF_UP);
        BigDecimal gananciaAbs = costoConGanancia.subtract(costo).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gananciaPorcentaje = gananciaAbs
                .multiply(cien)
                .divide(costo, 2, RoundingMode.HALF_UP);

        // 10) Calcular gastos totales porcentaje
        BigDecimal gastosTotalPorcentaje = gastosSobreImpTotal.add(gastosSobrePVPTotal);

        return new PrecioCalculadoDTO(
                pvpSinPromocion,
                pvpInflado,
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
     * @return String con los nombres concatenados, o cadena vacía si la lista está vacía
     */
    private String formatearNombresConceptos(List<String> nombres) {
        if (nombres == null || nombres.isEmpty()) {
            return "";
        }
        return String.join(" + ", nombres);
    }

    /**
     * Aplica promociones al precio calculado:
     * 1. Primero aplica porcentaje_inflacion del canal (si existe)
     * 2. Luego aplica la promoción de producto_canal_promocion (si existe y está activa)
     * 
     * @param productoId ID del producto
     * @param canal      Canal con el porcentaje_inflacion
     * @param pvp        Precio calculado antes de aplicar promociones
     * @return Precio con promociones aplicadas
     */
    private BigDecimal aplicarPromocion(Integer productoId, Canal canal, BigDecimal pvp) {
        BigDecimal resultado = pvp;
        BigDecimal cien = BigDecimal.valueOf(100);

        // 1) Aplicar porcentaje_inflacion del canal (si existe)
        // El porcentaje se obtiene del campo porcentaje_inflacion de la tabla canales
        // Por defecto es 0 (no afecta el cálculo)
        // Para ML típicamente es 10 (10% de gasto fijo, equivalente a dividir por 0.9)
        // Fórmula: divisor = 1 - (porcentaje / 100)
        if (canal != null && canal.getPorcentajeInflacion() != null) {
            BigDecimal porcentajeInflacion = canal.getPorcentajeInflacion();
            if (porcentajeInflacion.compareTo(BigDecimal.ZERO) > 0) {
                // Convertir porcentaje a divisor: divisor = 1 - (porcentaje / 100)
                BigDecimal divisor = BigDecimal.ONE.subtract(porcentajeInflacion.divide(cien, 6, RoundingMode.HALF_UP));
                if (divisor.compareTo(BigDecimal.ZERO) > 0) {
                    resultado = resultado.divide(divisor, 6, RoundingMode.HALF_UP);
                }
            }
        }

        // 2) Aplicar promoción de producto_canal_promocion (si existe y está activa)
        Optional<ProductoCanalPromocion> promocionOpt = Optional.empty();
        if (canal != null && canal.getId() != null) {
            promocionOpt = productoCanalPromocionRepository
                    .findByProductoIdAndCanalId(productoId, canal.getId());
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
                                if (valor.compareTo(BigDecimal.ZERO) > 0 && valor.compareTo(cien) < 0) {
                                    BigDecimal promocionFrac = valor.divide(cien, 6, RoundingMode.HALF_UP);
                                    BigDecimal denominador = BigDecimal.ONE.subtract(promocionFrac);
                                    if (denominador.compareTo(BigDecimal.ZERO) > 0) {
                                        resultado = resultado.divide(denominador, 6, RoundingMode.HALF_UP);
                                    }
                                }
                                break;

                            case DIVISOR:
                                // Divisor: precio / valor
                                // Ejemplo: valor = 0.9 divide el precio por 0.9 (equivalente a multiplicar por 1.11)
                                if (valor.compareTo(BigDecimal.ZERO) > 0) {
                                    resultado = resultado.divide(valor, 6, RoundingMode.HALF_UP);
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
        ProductoCanal productoCanal = obtenerProductoCanal(idProducto, idCanal);
        String nombreCanal = productoCanal.getCanal().getCanal();
        boolean esCanalNube = nombreCanal != null && (nombreCanal.equalsIgnoreCase("KT HOGAR")
                || nombreCanal.equalsIgnoreCase("KT GASTRO"));

        List<ConceptoGasto> conceptos = obtenerConceptosAplicables(
                idCanal,
                numeroCuotas,
                esCanalNube,
                producto);

        List<CanalConcepto> conceptosCanal = convertirConceptosACanalConcepto(conceptos, idCanal);

        if (nombreCanal != null && nombreCanal.equalsIgnoreCase("KT HOGAR")) {
            return generarFormulaKTHogar(producto, productoCanal, conceptosCanal, numeroCuotas);
        } else if (nombreCanal != null && nombreCanal.equalsIgnoreCase("KT GASTRO")) {
            return generarFormulaKTGastro(producto, productoCanal, conceptosCanal, numeroCuotas);
        } else {
            return generarFormulaGeneral(producto, productoCanal, conceptosCanal, numeroCuotas);
        }
    }

    private FormulaCalculoDTO generarFormulaKTHogar(
            Producto producto,
            ProductoCanal productoCanal,
            List<CanalConcepto> conceptos,
            Integer numeroCuotas) {
        List<FormulaCalculoDTO.PasoCalculo> pasos = new java.util.ArrayList<>();
        BigDecimal cien = BigDecimal.valueOf(100);

        // Paso 1: Costo
        BigDecimal costo = producto.getCosto();
        pasos.add(new FormulaCalculoDTO.PasoCalculo(1, "Costo base del producto",
                "COSTO", costo, String.format("Costo: $%s", costo)));

        // Paso 2: IVA e IMP
        BigDecimal iva = producto.getIva();
        List<CanalConcepto> gastosSobreImp = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.IMP)
                .collect(Collectors.toList());
        BigDecimal gastosSobreImpTotal = calcularGastosPorcentaje(gastosSobreImp);
        BigDecimal imp = BigDecimal.ONE
                .add(iva.divide(cien, 6, RoundingMode.HALF_UP))
                .add(gastosSobreImpTotal.divide(cien, 6, RoundingMode.HALF_UP));

        List<String> nombresConceptosImp = obtenerNombresConceptos(conceptos, AplicaSobre.IMP);
        String nombresImpFormateados = formatearNombresConceptos(nombresConceptosImp);
        String formulaImp = nombresImpFormateados.isEmpty()
                ? "IMP = 1 + IVA/100"
                : String.format("IMP = 1 + IVA/100 + %s/100", nombresImpFormateados);

        String detalleImp = String.format("IVA: %s%%", iva);
        if (gastosSobreImpTotal.compareTo(BigDecimal.ZERO) > 0 && !nombresImpFormateados.isEmpty()) {
            detalleImp += String.format(" + %s: %s%%", nombresImpFormateados, gastosSobreImpTotal);
        }
        detalleImp += String.format(" → IMP = 1 + %s/100", iva);
        if (gastosSobreImpTotal.compareTo(BigDecimal.ZERO) > 0) {
            detalleImp += String.format(" + %s/100 = %s", gastosSobreImpTotal, imp);
        } else {
            detalleImp += String.format(" = %s", imp);
        }
        pasos.add(new FormulaCalculoDTO.PasoCalculo(2, "Factor de impuestos (IMP)",
                formulaImp, imp, detalleImp));

        // Paso 3: Ganancia
        BigDecimal margenPorcentaje = productoCanal.getMargenPorcentaje() != null
                ? productoCanal.getMargenPorcentaje()
                : BigDecimal.ZERO;
        List<CanalConcepto> gastosSobreCostoMargen = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.COSTO_MARGEN)
                .collect(Collectors.toList());
        BigDecimal conceptoCostoMargen = calcularGastosPorcentaje(gastosSobreCostoMargen);
        BigDecimal gananciaUsar = margenPorcentaje.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : margenPorcentaje.add(conceptoCostoMargen);

        List<String> nombresConceptosCostoMargen = obtenerNombresConceptos(conceptos, AplicaSobre.COSTO_MARGEN);
        String nombresCostoMargenFormateados = formatearNombresConceptos(nombresConceptosCostoMargen);
        String formulaGanancia = nombresCostoMargenFormateados.isEmpty()
                ? "GANANCIA = MARGEN"
                : String.format("GANANCIA = MARGEN + %s", nombresCostoMargenFormateados);

        String detalleGanancia = String.format("MARGEN: %s%%", margenPorcentaje);
        if (conceptoCostoMargen.compareTo(BigDecimal.ZERO) > 0 && !nombresCostoMargenFormateados.isEmpty()) {
            detalleGanancia += String.format(" + %s: %s%%", nombresCostoMargenFormateados, conceptoCostoMargen);
        }
        detalleGanancia += String.format(" → GANANCIA = %s%%", gananciaUsar);
        pasos.add(new FormulaCalculoDTO.PasoCalculo(3, "Ganancia",
                formulaGanancia, gananciaUsar, detalleGanancia));

        // Paso 4: Costo con ganancia
        BigDecimal gananciaFrac = gananciaUsar.divide(cien, 6, RoundingMode.HALF_UP);
        BigDecimal costoConGanancia = costo.multiply(BigDecimal.ONE.add(gananciaFrac));
        pasos.add(new FormulaCalculoDTO.PasoCalculo(4, "Costo con ganancia",
                "COSTO_CON_GANANCIA = COSTO * (1 + GANANCIA/100)",
                costoConGanancia,
                String.format("%s * (1 + %s/100) = %s", costo, gananciaUsar, costoConGanancia)));

        // Paso 5: Costo con impuestos
        BigDecimal costoConImpuestos = costoConGanancia.multiply(imp);
        pasos.add(new FormulaCalculoDTO.PasoCalculo(5, "Costo con impuestos",
                "COSTO_CON_IMPUESTOS = COSTO_CON_GANANCIA * IMP",
                costoConImpuestos,
                String.format("%s * %s = %s", costoConGanancia, imp, costoConImpuestos)));

        // Paso 6: GTN
        BigDecimal porcentajeConceptosPVP = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.PVP)
                .map(cc -> cc.getConcepto().getPorcentaje())
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal porcentajeCuota = BigDecimal.ZERO;
        if (numeroCuotas != null && numeroCuotas > 0) {
            Integer idCanal = productoCanal.getCanal().getId();
            List<CanalConceptoCuota> cuotasCanal = canalConceptoCuotaRepository.findByCanalIdAndCuotas(idCanal,
                    numeroCuotas);
            Canal canalActual = canalRepository.findById(idCanal).orElse(null);
            if (canalActual != null && canalActual.getCanalBase() != null) {
                cuotasCanal.addAll(canalConceptoCuotaRepository
                        .findByCanalIdAndCuotas(canalActual.getCanalBase().getId(), numeroCuotas));
            }
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
        }

        BigDecimal gtn = porcentajeConceptosPVP.add(porcentajeCuota);
        List<String> nombresConceptosPVP = obtenerNombresConceptos(conceptos, AplicaSobre.PVP);
        String nombresPVPFormateados = formatearNombresConceptos(nombresConceptosPVP);
        String formulaGTN = nombresPVPFormateados.isEmpty()
                ? (porcentajeCuota.compareTo(BigDecimal.ZERO) > 0
                        ? String.format("GTN = %s cuotas", numeroCuotas)
                        : "GTN = 0")
                : (porcentajeCuota.compareTo(BigDecimal.ZERO) > 0
                        ? String.format("GTN = %s + %s cuotas", nombresPVPFormateados, numeroCuotas)
                        : String.format("GTN = %s", nombresPVPFormateados));

        String detalleGTN = "";
        if (!nombresPVPFormateados.isEmpty() && porcentajeConceptosPVP.compareTo(BigDecimal.ZERO) > 0) {
            detalleGTN = String.format("Conceptos PVP (%s): %s%%", nombresPVPFormateados, porcentajeConceptosPVP);
        } else if (porcentajeConceptosPVP.compareTo(BigDecimal.ZERO) > 0) {
            detalleGTN = String.format("Conceptos PVP: %s%%", porcentajeConceptosPVP);
        }
        if (porcentajeCuota.compareTo(BigDecimal.ZERO) > 0) {
            if (!detalleGTN.isEmpty()) {
                detalleGTN += " + ";
            }
            detalleGTN += String.format("%s cuotas: %s%%", numeroCuotas, porcentajeCuota);
        }
        if (detalleGTN.isEmpty()) {
            detalleGTN = "GTN = 0%";
        } else {
            detalleGTN += String.format(" → GTN = %s%%", gtn);
        }
        pasos.add(new FormulaCalculoDTO.PasoCalculo(6, "GTN (Gastos Totales Netos)",
                formulaGTN, gtn, detalleGTN));

        // Paso 7: CUPON
        List<CanalConcepto> conceptosCupon = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.CUPON)
                .collect(Collectors.toList());
        BigDecimal porcentajeCupon = calcularGastosPorcentaje(conceptosCupon);
        List<String> nombresConceptosCupon = obtenerNombresConceptos(conceptos, AplicaSobre.CUPON);
        String nombresCuponFormateados = formatearNombresConceptos(nombresConceptosCupon);
        String formulaCupon = nombresCuponFormateados.isEmpty()
                ? "CUPON = 0"
                : String.format("CUPON = %s", nombresCuponFormateados);
        pasos.add(new FormulaCalculoDTO.PasoCalculo(7, "CUPON",
                formulaCupon, porcentajeCupon,
                porcentajeCupon.compareTo(BigDecimal.ZERO) > 0
                        ? String.format("CUPON = %s = %s%%", nombresCuponFormateados, porcentajeCupon)
                        : "CUPON = 0%"));

        // Paso 8: PVP final
        BigDecimal gtnFrac = gtn.divide(cien, 10, RoundingMode.HALF_UP);
        BigDecimal denominadorGTN = BigDecimal.ONE.subtract(gtnFrac);
        BigDecimal denominadorCombinado = denominadorGTN;
        if (porcentajeCupon.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal cuponFrac = porcentajeCupon.divide(cien, 10, RoundingMode.HALF_UP);
            BigDecimal denominadorCupon = BigDecimal.ONE.subtract(cuponFrac);
            denominadorCombinado = denominadorGTN.multiply(denominadorCupon);
        }
        BigDecimal pvp = costoConImpuestos.divide(denominadorCombinado, 10, RoundingMode.HALF_UP);
        BigDecimal pvpSinPromocion = pvp.setScale(2, RoundingMode.HALF_UP);

        String formulaPVP = "PVP = COSTO_CON_IMPUESTOS / ((1 - GTN/100) * (1 - CUPON/100))";
        String detallePVP = String.format("%s / ((1 - %s/100) * (1 - %s/100)) = %s",
                costoConImpuestos, gtn, porcentajeCupon, pvpSinPromocion);
        pasos.add(new FormulaCalculoDTO.PasoCalculo(8, "PVP sin promociones",
                formulaPVP, pvpSinPromocion, detallePVP));

        // Paso 9: Aplicar porcentaje_inflacion del canal (si existe)
        BigDecimal pvpConInflacion = pvpSinPromocion;
        Canal canal = productoCanal.getCanal();
        int pasoNumeroPromo = 9;
        if (canal != null && canal.getPorcentajeInflacion() != null
                && canal.getPorcentajeInflacion().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal porcentajeInflacion = canal.getPorcentajeInflacion();
            BigDecimal divisorInflacion = BigDecimal.ONE
                    .subtract(porcentajeInflacion.divide(cien, 6, RoundingMode.HALF_UP));
            pvpConInflacion = pvpSinPromocion.divide(divisorInflacion, 6, RoundingMode.HALF_UP);
            pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumeroPromo++, "Aplicar porcentaje inflación del canal",
                    "PVP = PVP_SIN_PROMOCION / (1 - PORCENTAJE_INFLACION/100)",
                    pvpConInflacion,
                    String.format("Inflación canal: %s%% → %s / (1 - %s/100) = %s",
                            porcentajeInflacion, pvpSinPromocion, porcentajeInflacion, pvpConInflacion)));
        }

        // Paso 10: Aplicar promoción de producto_canal_promocion (si existe y está activa)
        BigDecimal pvpInflado = pvpConInflacion;
        if (canal != null) {
            Optional<ProductoCanalPromocion> promocionOpt = productoCanalPromocionRepository
                    .findByProductoIdAndCanalId(producto.getId(), canal.getId());
            if (promocionOpt.isPresent()) {
                ProductoCanalPromocion promocion = promocionOpt.get();
                if (promocion.getActiva() != null && promocion.getActiva()) {
                    LocalDate hoy = LocalDate.now();
                    boolean fechaValida = true;
                    if (promocion.getFechaDesde() != null && hoy.isBefore(promocion.getFechaDesde())) {
                        fechaValida = false;
                    }
                    if (promocion.getFechaHasta() != null && hoy.isAfter(promocion.getFechaHasta())) {
                        fechaValida = false;
                    }

                    if (fechaValida) {
                        Promocion promocionMaestra = promocion.getPromocion();
                        if (promocionMaestra != null) {
                            TipoPromocionTabla tipo = promocionMaestra.getTipo();
                            BigDecimal valor = promocionMaestra.getValor();
                            BigDecimal pvpAntesPromo = pvpInflado;

                            switch (tipo) {
                                case MULTIPLICADOR:
                                    if (valor.compareTo(BigDecimal.ZERO) > 0) {
                                        pvpInflado = pvpInflado.multiply(valor);
                                        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumeroPromo++,
                                                "Inflar precio (multiplicador)",
                                                "PVP = PVP * VALOR",
                                                pvpInflado,
                                                String.format("Multiplicador: %s → %s * %s = %s",
                                                        valor, pvpAntesPromo, valor, pvpInflado)));
                                    }
                                    break;

                                case DESCUENTO_PORC:
                                    if (valor.compareTo(BigDecimal.ZERO) > 0 && valor.compareTo(cien) < 0) {
                                        BigDecimal promocionFrac = valor.divide(cien, 6, RoundingMode.HALF_UP);
                                        BigDecimal denominadorPromo = BigDecimal.ONE.subtract(promocionFrac);
                                        pvpInflado = pvpInflado.divide(denominadorPromo, 6, RoundingMode.HALF_UP);
                                        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumeroPromo++,
                                                "Inflar precio (promoción porcentual)",
                                                "PVP = PVP / (1 - VALOR/100)",
                                                pvpInflado,
                                                String.format("Inflación: %s%% → %s / (1 - %s/100) = %s",
                                                        valor, pvpAntesPromo, valor, pvpInflado)));
                                    }
                                    break;

                                case DIVISOR:
                                    if (valor.compareTo(BigDecimal.ZERO) > 0) {
                                        pvpInflado = pvpInflado.divide(valor, 6, RoundingMode.HALF_UP);
                                        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumeroPromo++,
                                                "Inflar precio (divisor)",
                                                "PVP = PVP / VALOR",
                                                pvpInflado,
                                                String.format("Divisor: %s → %s / %s = %s",
                                                        valor, pvpAntesPromo, valor, pvpInflado)));
                                    }
                                    break;

                                case PRECIO_FIJO:
                                    if (valor.compareTo(BigDecimal.ZERO) > 0) {
                                        pvpInflado = valor;
                                        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumeroPromo++,
                                                "Establecer precio fijo",
                                                "PVP = VALOR",
                                                pvpInflado,
                                                String.format("Precio fijo: %s", valor)));
                                    }
                                    break;

                                default:
                                    break;
                            }
                        }
                    }
                }
            }
        }

        pvpInflado = pvpInflado.setScale(2, RoundingMode.HALF_UP);

        // Construir fórmula general usando nombres definidos en los pasos anteriores
        StringBuilder formulaGeneralBuilder = new StringBuilder("PVP = (((COSTO + COSTO * GANANCIA) * IMP");

        // Agregar nombres de conceptos IMP si existen (aunque IMP ya está definido, mostramos los conceptos que lo componen)
        if (!nombresImpFormateados.isEmpty()) {
            formulaGeneralBuilder.append(" (").append(nombresImpFormateados).append(")");
        }

        formulaGeneralBuilder.append(") / (1 - GTN/100)) / (1 - CUPON/100)");

        String formulaGeneral = formulaGeneralBuilder.toString();

        return new FormulaCalculoDTO(
                productoCanal.getCanal().getCanal(),
                numeroCuotas,
                formulaGeneral,
                pasos,
                pvpInflado);
    }

    /**
     * Genera la fórmula de cálculo para el canal KT GASTRO.
     * 
     * Fórmula: PVP GASTRO = SI( [@TAG]="MAQUINA"; 
     *     ( [@[PVP ML]] * (1-0,145) ); 
     *     ( ( [@COSTO] * (1+([@[GAN.MIN.ML]] * (1-REL_ML_KTG))) * [@[IMP.]] ) / (1-(MARKETING+EMBALAJE+GASTONUBE)) ) )
     */
    private FormulaCalculoDTO generarFormulaKTGastro(
            Producto producto,
            ProductoCanal productoCanal,
            List<CanalConcepto> conceptos,
            Integer numeroCuotas) {
        // Verificar si es máquina
        boolean esMaquina = esProductoMaquina(producto);

        if (esMaquina) {
            return generarFormulaKTGastroMaquina(producto, productoCanal, numeroCuotas);
        } else {
            return generarFormulaKTGastroNoMaquina(producto, productoCanal, conceptos, numeroCuotas);
        }
    }

    /**
     * Genera la fórmula cuando el producto es máquina.
     * Fórmula: PVP GASTRO = PVP ML * (1 - 0.145)
     */
    private FormulaCalculoDTO generarFormulaKTGastroMaquina(
            Producto producto,
            ProductoCanal productoCanal,
            Integer numeroCuotas) {
        List<FormulaCalculoDTO.PasoCalculo> pasos = new java.util.ArrayList<>();
        int pasoNumero = 1;

        // Paso 1: Información sobre el producto
        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                "Producto es máquina",
                "TAG = MAQUINA",
                BigDecimal.ONE,
                "El producto pertenece a una clasificación gastronómica marcada como máquina"));

        // Paso 2: Obtener PVP ML
        Canal canalML = canalRepository.findByCanalIgnoreCase("ML")
                .orElseThrow(() -> new NotFoundException("No se encontró el canal ML"));

        ProductoCanal productoCanalML = productoCanalRepository
                .findByProductoIdAndCanalId(producto.getId(), canalML.getId())
                .orElseThrow(() -> new NotFoundException(
                        "No se encontró la configuración del producto para el canal ML"));

        // Obtener conceptos para ML
        boolean esCanalNube = false;
        List<ConceptoGasto> conceptosML = obtenerConceptosAplicables(
                canalML.getId(),
                numeroCuotas,
                esCanalNube,
                producto);

        List<CanalConcepto> conceptosCanalML = convertirConceptosACanalConcepto(conceptosML, canalML.getId());

        // Calcular PVP ML usando el método de fórmula general
        FormulaCalculoDTO formulaML = generarFormulaGeneral(producto, productoCanalML, conceptosCanalML, numeroCuotas);
        BigDecimal pvpML = formulaML.resultadoFinal();

        // Agregar pasos de ML con numeración ajustada
        for (FormulaCalculoDTO.PasoCalculo pasoML : formulaML.pasos()) {
            pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                    "Cálculo ML: " + pasoML.descripcion(),
                    pasoML.formula(),
                    pasoML.valor(),
                    pasoML.detalle()));
        }

        // Paso final: Aplicar descuento del 14.5%
        BigDecimal factorDescuento = BigDecimal.valueOf(0.855); // 1 - 0.145
        BigDecimal pvpGastro = pvpML.multiply(factorDescuento).setScale(2, RoundingMode.HALF_UP);

        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                "Aplicar descuento para KT GASTRO (máquina)",
                "PVP_GASTRO = PVP_ML * (1 - 0.145)",
                pvpGastro,
                String.format("PVP ML: %s * (1 - 0.145) = %s * 0.855 = %s",
                        pvpML, pvpML, pvpGastro)));

        // Aplicar promociones del canal KT GASTRO
        BigDecimal pvpInflado = aplicarPromocion(producto.getId(), productoCanal.getCanal(), pvpGastro);
        pvpInflado = pvpInflado.setScale(2, RoundingMode.HALF_UP);

        if (pvpInflado.compareTo(pvpGastro) != 0) {
            pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                    "Aplicar promociones del canal KT GASTRO",
                    "PVP_INFLADO = aplicarPromocion(PVP_GASTRO)",
                    pvpInflado,
                    String.format("PVP con promociones: %s", pvpInflado)));
        }

        String formulaGeneral = "PVP_GASTRO = PVP_ML * (1 - 0.145)";

        return new FormulaCalculoDTO(
                productoCanal.getCanal().getCanal(),
                numeroCuotas,
                formulaGeneral,
                pasos,
                pvpInflado);
    }

    /**
     * Genera la fórmula cuando el producto NO es máquina.
     * Fórmula: PVP GASTRO = (COSTO * (1 + (GAN.MIN.ML * (1 - REL_ML_KTG))) * IMP) / (1 - (MARKETING + EMBALAJE + GASTONUBE))
     */
    private FormulaCalculoDTO generarFormulaKTGastroNoMaquina(
            Producto producto,
            ProductoCanal productoCanal,
            List<CanalConcepto> conceptos,
            Integer numeroCuotas) {
        List<FormulaCalculoDTO.PasoCalculo> pasos = new java.util.ArrayList<>();
        BigDecimal cien = BigDecimal.valueOf(100);
        int pasoNumero = 1;

        // Paso 1: Costo
        BigDecimal costo = producto.getCosto();
        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++, "Costo base del producto",
                "COSTO", costo, String.format("Costo: $%s", costo)));

        // Paso 2: IVA e IMP
        BigDecimal iva = producto.getIva();
        List<CanalConcepto> gastosSobreImp = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.IMP)
                .collect(Collectors.toList());
        BigDecimal gastosSobreImpTotal = calcularGastosPorcentaje(gastosSobreImp);
        BigDecimal imp = BigDecimal.ONE
                .add(iva.divide(cien, 6, RoundingMode.HALF_UP))
                .add(gastosSobreImpTotal.divide(cien, 6, RoundingMode.HALF_UP));

        List<String> nombresConceptosImp = obtenerNombresConceptos(conceptos, AplicaSobre.IMP);
        String nombresImpFormateados = formatearNombresConceptos(nombresConceptosImp);
        String formulaImp = nombresImpFormateados.isEmpty()
                ? "IMP = 1 + IVA/100"
                : String.format("IMP = 1 + IVA/100 + %s/100", nombresImpFormateados);

        String detalleImp = String.format("IVA: %s%%", iva);
        if (gastosSobreImpTotal.compareTo(BigDecimal.ZERO) > 0 && !nombresImpFormateados.isEmpty()) {
            detalleImp += String.format(" + %s: %s%%", nombresImpFormateados, gastosSobreImpTotal);
        }
        detalleImp += String.format(" → IMP = 1 + %s/100", iva);
        if (gastosSobreImpTotal.compareTo(BigDecimal.ZERO) > 0) {
            detalleImp += String.format(" + %s/100 = %s", gastosSobreImpTotal, imp);
        } else {
            detalleImp += String.format(" = %s", imp);
        }
        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++, "Factor de impuestos (IMP)",
                formulaImp, imp, detalleImp));

        // Paso 3: Obtener GAN.MIN.ML del canal ML
        Canal canalML = canalRepository.findByCanalIgnoreCase("ML")
                .orElseThrow(() -> new NotFoundException("No se encontró el canal ML"));

        ProductoCanal productoCanalML = productoCanalRepository
                .findByProductoIdAndCanalId(producto.getId(), canalML.getId())
                .orElse(null);

        BigDecimal gananciaMinML = BigDecimal.ZERO;
        if (productoCanalML != null && productoCanalML.getMargenPorcentaje() != null) {
            gananciaMinML = productoCanalML.getMargenPorcentaje();
        }

        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                "Ganancia mínima ML (GAN.MIN.ML)",
                "GAN.MIN.ML",
                gananciaMinML,
                String.format("Margen porcentaje del canal ML: %s%%", gananciaMinML)));

        // Paso 4: Obtener REL_ML_KTG
        List<CanalConcepto> conceptosRelMLKTG = conceptos.stream()
                .filter(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.COSTO_MARGEN)
                .collect(Collectors.toList());

        BigDecimal relMLKTG = calcularGastosPorcentaje(conceptosRelMLKTG);
        List<String> nombresRelMLKTG = obtenerNombresConceptos(conceptos, AplicaSobre.COSTO_MARGEN);
        String nombresRelMLKTGFormateados = formatearNombresConceptos(nombresRelMLKTG);

        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                "Relación ML-KT GASTRO (REL_ML_KTG)",
                nombresRelMLKTGFormateados.isEmpty() ? "REL_ML_KTG = 0" : String.format("REL_ML_KTG = %s", nombresRelMLKTGFormateados),
                relMLKTG,
                nombresRelMLKTGFormateados.isEmpty()
                        ? String.format("REL_ML_KTG = %s%%", relMLKTG)
                        : String.format("%s = %s%%", nombresRelMLKTGFormateados, relMLKTG)));

        // Paso 5: Calcular ganancia ajustada: GAN.MIN.ML * (1 - REL_ML_KTG)
        BigDecimal factorRelacion = BigDecimal.ONE.subtract(relMLKTG.divide(cien, 6, RoundingMode.HALF_UP));
        BigDecimal gananciaAjustada = gananciaMinML.multiply(factorRelacion);

        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                "Ganancia ajustada",
                "GANANCIA_AJUSTADA = GAN.MIN.ML * (1 - REL_ML_KTG/100)",
                gananciaAjustada,
                String.format("%s%% * (1 - %s/100) = %s%% * %s = %s%%",
                        gananciaMinML, relMLKTG, gananciaMinML, factorRelacion, gananciaAjustada)));

        // Paso 6: Calcular costo con ganancia: COSTO * (1 + GANANCIA_AJUSTADA/100)
        BigDecimal gananciaFrac = gananciaAjustada.divide(cien, 6, RoundingMode.HALF_UP);
        BigDecimal costoConGanancia = costo.multiply(BigDecimal.ONE.add(gananciaFrac));

        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                "Costo con ganancia",
                "COSTO_CON_GANANCIA = COSTO * (1 + GANANCIA_AJUSTADA/100)",
                costoConGanancia,
                String.format("%s * (1 + %s/100) = %s * %s = %s",
                        costo, gananciaAjustada, costo, BigDecimal.ONE.add(gananciaFrac), costoConGanancia)));

        // Paso 7: Aplicar impuestos: * IMP
        BigDecimal costoConImpuestos = costoConGanancia.multiply(imp);

        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                "Costo con impuestos",
                "COSTO_CON_IMPUESTOS = COSTO_CON_GANANCIA * IMP",
                costoConImpuestos,
                String.format("%s * %s = %s", costoConGanancia, imp, costoConImpuestos)));

        // Paso 8: Obtener conceptos MARKETING, EMBALAJE, GASTONUBE (aplica_sobre='PVP')
        List<CanalConcepto> gastosSobrePVP = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.PVP)
                .collect(Collectors.toList());

        BigDecimal gastosSobrePVPTotal = calcularGastosPorcentaje(gastosSobrePVP);
        List<String> nombresConceptosPVP = obtenerNombresConceptos(conceptos, AplicaSobre.PVP);
        String nombresPVPFormateados = formatearNombresConceptos(nombresConceptosPVP);

        String formulaGastosPVP = nombresPVPFormateados.isEmpty()
                ? "GASTOS_PVP = 0"
                : String.format("GASTOS_PVP = %s", nombresPVPFormateados);

        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                "Gastos sobre PVP (MARKETING + EMBALAJE + GASTONUBE)",
                formulaGastosPVP,
                gastosSobrePVPTotal,
                nombresPVPFormateados.isEmpty()
                        ? String.format("Gastos PVP: %s%%", gastosSobrePVPTotal)
                        : String.format("Gastos PVP (%s): %s%%", nombresPVPFormateados, gastosSobrePVPTotal)));

        // Paso 9: Calcular PVP: costoConImpuestos / (1 - GASTOS_PVP/100)
        BigDecimal gastosSobrePVPFrac = gastosSobrePVPTotal.divide(cien, 6, RoundingMode.HALF_UP);
        BigDecimal denominador = BigDecimal.ONE.subtract(gastosSobrePVPFrac);
        BigDecimal pvp = costoConImpuestos.divide(denominador, 6, RoundingMode.HALF_UP);
        BigDecimal pvpSinPromocion = pvp.setScale(2, RoundingMode.HALF_UP);

        String formulaPVP = nombresPVPFormateados.isEmpty()
                ? "PVP = COSTO_CON_IMPUESTOS / (1 - GASTOS_PVP/100)"
                : String.format("PVP = COSTO_CON_IMPUESTOS / (1 - %s/100)", nombresPVPFormateados);

        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                "PVP sin promociones",
                formulaPVP,
                pvpSinPromocion,
                String.format("%s / (1 - %s/100) = %s",
                        costoConImpuestos, gastosSobrePVPTotal, pvpSinPromocion)));

        // Paso 10: Aplicar promociones
        BigDecimal pvpInflado = aplicarPromocion(producto.getId(), productoCanal.getCanal(), pvpSinPromocion);
        pvpInflado = pvpInflado.setScale(2, RoundingMode.HALF_UP);

        if (pvpInflado.compareTo(pvpSinPromocion) != 0) {
            pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                    "Aplicar promociones del canal KT GASTRO",
                    "PVP_INFLADO = aplicarPromocion(PVP_SIN_PROMOCION)",
                    pvpInflado,
                    String.format("PVP con promociones: %s", pvpInflado)));
        }

        // Construir fórmula general
        StringBuilder formulaGeneralBuilder = new StringBuilder("PVP = (COSTO * (1 + (GAN.MIN.ML * (1 - REL_ML_KTG/100))) * IMP) / (1 - GASTOS_PVP/100)");
        if (!nombresPVPFormateados.isEmpty()) {
            formulaGeneralBuilder.append(" donde GASTOS_PVP = ").append(nombresPVPFormateados);
        }

        return new FormulaCalculoDTO(
                productoCanal.getCanal().getCanal(),
                numeroCuotas,
                formulaGeneralBuilder.toString(),
                pasos,
                pvpInflado);
    }

    private FormulaCalculoDTO generarFormulaGeneral(
            Producto producto,
            ProductoCanal productoCanal,
            List<CanalConcepto> conceptos,
            Integer numeroCuotas) {
        List<FormulaCalculoDTO.PasoCalculo> pasos = new java.util.ArrayList<>();
        BigDecimal cien = BigDecimal.valueOf(100);
        String nombreCanal = productoCanal.getCanal().getCanal();
        int pasoNumero = 1;

        // Paso 1: Costo
        BigDecimal costo = producto.getCosto();
        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++, "Costo base del producto",
                "COSTO", costo, String.format("Costo: $%s", costo)));

        // Paso 2: IVA e IMP
        BigDecimal iva = producto.getIva();
        List<CanalConcepto> gastosSobreImp = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.IMP)
                .collect(Collectors.toList());
        BigDecimal gastosSobreImpTotal = calcularGastosPorcentaje(gastosSobreImp);
        BigDecimal imp = BigDecimal.ONE
                .add(iva.divide(cien, 6, RoundingMode.HALF_UP))
                .add(gastosSobreImpTotal.divide(cien, 6, RoundingMode.HALF_UP));

        List<String> nombresConceptosImp = obtenerNombresConceptos(conceptos, AplicaSobre.IMP);
        String nombresImpFormateados = formatearNombresConceptos(nombresConceptosImp);
        String formulaImp = nombresImpFormateados.isEmpty()
                ? "IMP = 1 + IVA/100"
                : String.format("IMP = 1 + IVA/100 + %s/100", nombresImpFormateados);

        String detalleImp = String.format("IVA: %s%%", iva);
        if (gastosSobreImpTotal.compareTo(BigDecimal.ZERO) > 0 && !nombresImpFormateados.isEmpty()) {
            detalleImp += String.format(" + %s: %s%%", nombresImpFormateados, gastosSobreImpTotal);
        }
        detalleImp += String.format(" → IMP = 1 + %s/100", iva);
        if (gastosSobreImpTotal.compareTo(BigDecimal.ZERO) > 0) {
            detalleImp += String.format(" + %s/100 = %s", gastosSobreImpTotal, imp);
        } else {
            detalleImp += String.format(" = %s", imp);
        }
        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++, "Factor de impuestos (IMP)",
                formulaImp, imp, detalleImp));

        // Paso 3: Margen y gastos sobre costo
        BigDecimal margenPorcentaje = productoCanal.getMargenPorcentaje() != null
                ? productoCanal.getMargenPorcentaje()
                : BigDecimal.ZERO;

        List<CanalConcepto> gastosSobreCosto = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.COSTO)
                .collect(Collectors.toList());
        BigDecimal gastosSobreCostoTotal = calcularGastosPorcentaje(gastosSobreCosto);

        BigDecimal costoConGastos = costo.multiply(
                BigDecimal.ONE.add(gastosSobreCostoTotal.divide(cien, 6, RoundingMode.HALF_UP)));

        List<String> nombresConceptosCosto = obtenerNombresConceptos(conceptos, AplicaSobre.COSTO);
        String nombresCostoFormateados = formatearNombresConceptos(nombresConceptosCosto);
        String nombreGastosCosto = nombresCostoFormateados.isEmpty() ? "GASTOS_COSTO" : nombresCostoFormateados;
        String formulaGastosCosto = String.format("COSTO_CON_GASTOS = COSTO * (1 + %s/100)", nombreGastosCosto);

        String detalleGastosCosto = gastosSobreCostoTotal.compareTo(BigDecimal.ZERO) > 0
                ? String.format("Gastos sobre COSTO (%s): %s%%", nombresCostoFormateados, gastosSobreCostoTotal)
                : "Sin gastos sobre COSTO";
        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++, "Costo con gastos",
                formulaGastosCosto,
                costoConGastos, detalleGastosCosto));

        // Paso 4: Costo con ganancia
        BigDecimal margenFrac = margenPorcentaje.divide(cien, 6, RoundingMode.HALF_UP);
        BigDecimal costoConGanancia = costoConGastos.multiply(BigDecimal.ONE.add(margenFrac));
        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++, "Costo con ganancia",
                "COSTO_CON_GANANCIA = COSTO_CON_GASTOS * (1 + MARGEN/100)",
                costoConGanancia,
                String.format("MARGEN: %s%% → %s * (1 + %s/100) = %s",
                        margenPorcentaje, costoConGastos, margenPorcentaje, costoConGanancia)));

        // Paso 5: Gastos sobre COSTO_MARGEN
        List<CanalConcepto> gastosSobreCostoMargen = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.COSTO_MARGEN)
                .collect(Collectors.toList());
        BigDecimal gastosSobreCostoMargenTotal = calcularGastosPorcentaje(gastosSobreCostoMargen);
        BigDecimal costoConGananciaFinal = costoConGanancia;
        if (gastosSobreCostoMargenTotal.compareTo(BigDecimal.ZERO) > 0) {
            costoConGananciaFinal = costoConGanancia.multiply(
                    BigDecimal.ONE.add(gastosSobreCostoMargenTotal.divide(cien, 6, RoundingMode.HALF_UP)));
            List<String> nombresConceptosCostoMargen = obtenerNombresConceptos(conceptos, AplicaSobre.COSTO_MARGEN);
            String nombresCostoMargenFormateados = formatearNombresConceptos(nombresConceptosCostoMargen);
            String nombreGastosCostoMargen = nombresCostoMargenFormateados.isEmpty() ? "GASTOS_COSTO_MARGEN"
                    : nombresCostoMargenFormateados;
            String formulaCostoMargen = String.format("COSTO_CON_GANANCIA_FINAL = COSTO_CON_GANANCIA * (1 + %s/100)",
                    nombreGastosCostoMargen);
            pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++, "Gastos sobre COSTO_MARGEN",
                    formulaCostoMargen,
                    costoConGananciaFinal,
                    String.format("Gastos COSTO_MARGEN (%s): %s%% → %s * (1 + %s/100) = %s",
                            nombresCostoMargenFormateados, gastosSobreCostoMargenTotal, costoConGanancia,
                            gastosSobreCostoMargenTotal,
                            costoConGananciaFinal)));
        }

        // Paso 6: Envío (solo para ML)
        BigDecimal costoConGananciaYEnvio = costoConGananciaFinal;
        if (nombreCanal != null && nombreCanal.toUpperCase().contains("ML")) {
            BigDecimal precioEnvio = mlaRepository.findByProductoId(producto.getId())
                    .stream()
                    .findFirst()
                    .map(mla -> mla.getPrecioEnvio())
                    .filter(envio -> envio != null && envio.compareTo(BigDecimal.ZERO) > 0)
                    .orElse(BigDecimal.ZERO);

            if (precioEnvio.compareTo(BigDecimal.ZERO) > 0) {
                costoConGananciaYEnvio = costoConGananciaFinal.add(precioEnvio);
                pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++, "Sumar envío (ML)",
                        "COSTO_CON_GANANCIA_Y_ENVIO = COSTO_CON_GANANCIA_FINAL + ENVIO",
                        costoConGananciaYEnvio,
                        String.format("Envío: $%s → %s + %s = %s", precioEnvio, costoConGananciaFinal, precioEnvio,
                                costoConGananciaYEnvio)));
            }
        }

        // Paso 7: Aplicar impuestos (IVA)
        BigDecimal costoConImpuestos = costoConGananciaYEnvio.multiply(imp);
        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++, "Costo con impuestos",
                "COSTO_CON_IMPUESTOS = COSTO_CON_GANANCIA_Y_ENVIO * IMP",
                costoConImpuestos,
                String.format("%s * %s = %s", costoConGananciaYEnvio, imp, costoConImpuestos)));

        // Paso 8: Gastos sobre COSTO_IVA
        List<CanalConcepto> gastosSobreCostoIva = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.COSTO_IVA)
                .collect(Collectors.toList());
        BigDecimal gastosSobreCostoIvaTotal = calcularGastosPorcentaje(gastosSobreCostoIva);
        BigDecimal costoConImpuestosAntesIVA = costoConImpuestos;
        if (gastosSobreCostoIvaTotal.compareTo(BigDecimal.ZERO) > 0) {
            costoConImpuestos = costoConImpuestos.multiply(
                    BigDecimal.ONE.add(gastosSobreCostoIvaTotal.divide(cien, 6, RoundingMode.HALF_UP)));
            List<String> nombresConceptosCostoIva = obtenerNombresConceptos(conceptos, AplicaSobre.COSTO_IVA);
            String nombresCostoIvaFormateados = formatearNombresConceptos(nombresConceptosCostoIva);
            String nombreGastosCostoIva = nombresCostoIvaFormateados.isEmpty() ? "GASTOS_COSTO_IVA"
                    : nombresCostoIvaFormateados;
            String formulaCostoIva = String.format("COSTO_CON_IMPUESTOS = COSTO_CON_IMPUESTOS * (1 + %s/100)",
                    nombreGastosCostoIva);
            pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++, "Gastos sobre COSTO_IVA",
                    formulaCostoIva,
                    costoConImpuestos,
                    String.format("Gastos COSTO_IVA (%s): %s%% → %s * (1 + %s/100) = %s",
                            nombresCostoIvaFormateados, gastosSobreCostoIvaTotal, costoConImpuestosAntesIVA,
                            gastosSobreCostoIvaTotal,
                            costoConImpuestos)));
        }

        // Paso 9: Gastos sobre PVP (solo si NO hay cuotas)
        BigDecimal gastosSobrePVPTotal = BigDecimal.ZERO;
        BigDecimal gastosSobrePVPFrac = BigDecimal.ZERO;
        List<CanalConcepto> gastosSobrePVP = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.PVP)
                .collect(Collectors.toList());

        String nombreGastosPVP = "GASTOS_PVP";
        if (productoCanal.getAplicaCuotas() == null || !productoCanal.getAplicaCuotas()
                || numeroCuotas == null) {
            gastosSobrePVPTotal = calcularGastosPorcentaje(gastosSobrePVP);
            gastosSobrePVPFrac = gastosSobrePVPTotal.divide(cien, 6, RoundingMode.HALF_UP);
            if (gastosSobrePVPTotal.compareTo(BigDecimal.ZERO) > 0) {
                List<String> nombresConceptosPVP = obtenerNombresConceptos(conceptos, AplicaSobre.PVP);
                String nombresPVPFormateados = formatearNombresConceptos(nombresConceptosPVP);
                String formulaGastosPVP = nombresPVPFormateados.isEmpty()
                        ? "GASTOS_PVP = 0"
                        : String.format("GASTOS_PVP = %s", nombresPVPFormateados);
                pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++, "Gastos sobre PVP",
                        formulaGastosPVP, gastosSobrePVPTotal,
                        String.format("GASTOS_PVP = %s = %s%%", nombresPVPFormateados, gastosSobrePVPTotal)));
            }
        }

        // Paso 10: PVP base
        BigDecimal denominador = BigDecimal.ONE.subtract(gastosSobrePVPFrac);
        BigDecimal pvpBase = costoConImpuestos.divide(denominador, 6, RoundingMode.HALF_UP);
        String formulaPVPBase = String.format("PVP_BASE = COSTO_CON_IMPUESTOS / (1 - %s/100)", nombreGastosPVP);
        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++, "PVP base",
                formulaPVPBase,
                pvpBase,
                String.format("%s / (1 - %s/100) = %s", costoConImpuestos, gastosSobrePVPTotal, pvpBase)));

        // Paso 11: Aplicar porcentaje de cuotas (si aplicaCuotas es true)
        BigDecimal pvpDespuesCuotas = pvpBase;
        if (productoCanal.getAplicaCuotas() != null && productoCanal.getAplicaCuotas()
                && numeroCuotas != null) {
            Integer idCanal = productoCanal.getCanal().getId();
            List<CanalConcepto> todosConceptosCanal = canalConceptoRepository.findByCanalId(idCanal);
            Canal canalActual = canalRepository.findById(idCanal).orElse(null);
            if (canalActual != null && canalActual.getCanalBase() != null) {
                todosConceptosCanal.addAll(canalConceptoRepository.findByCanalId(canalActual.getCanalBase().getId()));
            }

            BigDecimal porcentajeConceptosCanal = todosConceptosCanal.stream()
                    .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.PVP)
                    .map(cc -> cc.getConcepto().getPorcentaje())
                    .filter(p -> p != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal porcentajeCuota = BigDecimal.ZERO;
            List<CanalConceptoCuota> cuotasCanal = canalConceptoCuotaRepository.findByCanalIdAndCuotas(idCanal,
                    numeroCuotas);
            if (canalActual != null && canalActual.getCanalBase() != null) {
                cuotasCanal.addAll(canalConceptoCuotaRepository
                        .findByCanalIdAndCuotas(canalActual.getCanalBase().getId(), numeroCuotas));
            }

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

            BigDecimal porcentajeCuotasTotal = porcentajeConceptosCanal.add(porcentajeCuota);
            if (porcentajeCuotasTotal.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal cuotasFrac = porcentajeCuotasTotal.divide(cien, 6, RoundingMode.HALF_UP);
                BigDecimal divisorCuotas = BigDecimal.ONE.subtract(cuotasFrac);
                pvpDespuesCuotas = pvpBase.divide(divisorCuotas, 6, RoundingMode.HALF_UP);
                List<String> nombresConceptosPVPCuotas = obtenerNombresConceptos(todosConceptosCanal, AplicaSobre.PVP);
                String nombresPVPCuotasFormateados = formatearNombresConceptos(nombresConceptosPVPCuotas);
                String nombreGTML = "GTML";
                String formulaGTML = nombresPVPCuotasFormateados.isEmpty()
                        ? (porcentajeCuota.compareTo(BigDecimal.ZERO) > 0
                                ? String.format("GTML = %s cuotas", numeroCuotas)
                                : "GTML = 0")
                        : (porcentajeCuota.compareTo(BigDecimal.ZERO) > 0
                                ? String.format("GTML = %s + %s cuotas", nombresPVPCuotasFormateados, numeroCuotas)
                                : String.format("GTML = %s", nombresPVPCuotasFormateados));
                String formulaCuotas = String.format("PVP = PVP_BASE / (1 - %s/100)", nombreGTML);
                String detalleCuotas = "";
                if (!nombresPVPCuotasFormateados.isEmpty() && porcentajeConceptosCanal.compareTo(BigDecimal.ZERO) > 0) {
                    detalleCuotas = String.format("Conceptos PVP (%s): %s%%", nombresPVPCuotasFormateados,
                            porcentajeConceptosCanal);
                } else if (porcentajeConceptosCanal.compareTo(BigDecimal.ZERO) > 0) {
                    detalleCuotas = String.format("Conceptos PVP: %s%%", porcentajeConceptosCanal);
                }
                if (porcentajeCuota.compareTo(BigDecimal.ZERO) > 0) {
                    if (!detalleCuotas.isEmpty()) {
                        detalleCuotas += " + ";
                    }
                    detalleCuotas += String.format("%s cuotas: %s%%", numeroCuotas, porcentajeCuota);
                }
                detalleCuotas += String.format(" → GTML = %s%%", porcentajeCuotasTotal);
                String detalleGTML = detalleCuotas.replace(String.format(" → GTML = %s%%", porcentajeCuotasTotal), "");
                pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++, "Calcular GTML",
                        formulaGTML,
                        porcentajeCuotasTotal,
                        String.format("GTML = %s = %s%%", detalleGTML, porcentajeCuotasTotal)));
                pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++, "PVP después de cuotas",
                        formulaCuotas,
                        pvpDespuesCuotas,
                        String.format("%s → %s", pvpBase, pvpDespuesCuotas)));
            }
        }

        // Paso 12: Aplicar reglas de descuento
        BigDecimal descuentoTotal = obtenerDescuentoAplicable(producto, productoCanal.getCanal().getId(),
                pvpDespuesCuotas);
        BigDecimal pvp = pvpDespuesCuotas;
        String nombreDescuento = "DESCUENTO";
        if (descuentoTotal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal descuentoFrac = descuentoTotal.divide(cien, 6, RoundingMode.HALF_UP);
            BigDecimal denominadorDescuento = BigDecimal.ONE.subtract(descuentoFrac);
            pvp = pvp.divide(denominadorDescuento, 6, RoundingMode.HALF_UP);
            pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++, "Aplicar descuento",
                    String.format("DESCUENTO = %s%%", descuentoTotal),
                    descuentoTotal,
                    String.format("DESCUENTO = %s%%", descuentoTotal)));
            pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++, "PVP después de descuento",
                    "PVP = PVP / (1 - DESCUENTO/100)",
                    pvp,
                    String.format("%s / (1 - %s/100) = %s",
                            pvpDespuesCuotas, descuentoTotal, pvp)));
        }

        // Paso 13: Aplicar márgenes adicionales (fijo)
        BigDecimal margenFijo = productoCanal.getMargenFijo() != null ? productoCanal.getMargenFijo() : BigDecimal.ZERO;
        String nombreMargenFijo = "MARGEN_FIJO";
        if (margenFijo.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal pvpAntesMargen = pvp;
            pvp = pvp.add(margenFijo);
            pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++, "Aplicar margen fijo",
                    String.format("MARGEN_FIJO = $%s", margenFijo),
                    margenFijo,
                    String.format("MARGEN_FIJO = $%s", margenFijo)));
            pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++, "PVP después de margen fijo",
                    "PVP = PVP + MARGEN_FIJO",
                    pvp,
                    String.format("%s + %s = %s", pvpAntesMargen, margenFijo, pvp)));
        }

        // Paso 14: Redondear PVP sin promociones
        BigDecimal pvpSinPromocion = pvp.setScale(2, RoundingMode.HALF_UP);

        // Paso 15: Aplicar porcentaje_inflacion del canal (si existe)
        BigDecimal pvpConInflacion = pvpSinPromocion;
        Canal canal = productoCanal.getCanal();
        if (canal != null && canal.getPorcentajeInflacion() != null
                && canal.getPorcentajeInflacion().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal porcentajeInflacion = canal.getPorcentajeInflacion();
            BigDecimal divisorInflacion = BigDecimal.ONE
                    .subtract(porcentajeInflacion.divide(cien, 6, RoundingMode.HALF_UP));
            pvpConInflacion = pvpSinPromocion.divide(divisorInflacion, 6, RoundingMode.HALF_UP);
            pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++, "Aplicar porcentaje inflación del canal",
                    "PVP = PVP_SIN_PROMOCION / (1 - PORCENTAJE_INFLACION/100)",
                    pvpConInflacion,
                    String.format("Inflación canal: %s%% → %s / (1 - %s/100) = %s",
                            porcentajeInflacion, pvpSinPromocion, porcentajeInflacion, pvpConInflacion)));
        }

        // Paso 16: Aplicar promoción de producto_canal_promocion (si existe y está activa)
        BigDecimal pvpInflado = pvpConInflacion;
        if (canal != null) {
            Optional<ProductoCanalPromocion> promocionOpt = productoCanalPromocionRepository
                    .findByProductoIdAndCanalId(producto.getId(), canal.getId());
            if (promocionOpt.isPresent()) {
                ProductoCanalPromocion promocion = promocionOpt.get();
                if (promocion.getActiva() != null && promocion.getActiva()) {
                    LocalDate hoy = LocalDate.now();
                    boolean fechaValida = true;
                    if (promocion.getFechaDesde() != null && hoy.isBefore(promocion.getFechaDesde())) {
                        fechaValida = false;
                    }
                    if (promocion.getFechaHasta() != null && hoy.isAfter(promocion.getFechaHasta())) {
                        fechaValida = false;
                    }

                    if (fechaValida) {
                        Promocion promocionMaestra = promocion.getPromocion();
                        if (promocionMaestra != null) {
                            TipoPromocionTabla tipo = promocionMaestra.getTipo();
                            BigDecimal valor = promocionMaestra.getValor();
                            BigDecimal pvpAntesPromo = pvpInflado;

                            switch (tipo) {
                                case MULTIPLICADOR:
                                    if (valor.compareTo(BigDecimal.ZERO) > 0) {
                                        pvpInflado = pvpInflado.multiply(valor);
                                        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                                                "Inflar precio (multiplicador)",
                                                "PVP = PVP * VALOR",
                                                pvpInflado,
                                                String.format("Multiplicador: %s → %s * %s = %s",
                                                        valor, pvpAntesPromo, valor, pvpInflado)));
                                    }
                                    break;

                                case DESCUENTO_PORC:
                                    if (valor.compareTo(BigDecimal.ZERO) > 0 && valor.compareTo(cien) < 0) {
                                        BigDecimal promocionFrac = valor.divide(cien, 6, RoundingMode.HALF_UP);
                                        BigDecimal denominadorPromo = BigDecimal.ONE.subtract(promocionFrac);
                                        pvpInflado = pvpInflado.divide(denominadorPromo, 6, RoundingMode.HALF_UP);
                                        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                                                "Inflar precio (promoción porcentual)",
                                                "PVP = PVP / (1 - VALOR/100)",
                                                pvpInflado,
                                                String.format("Inflación: %s%% → %s / (1 - %s/100) = %s",
                                                        valor, pvpAntesPromo, valor, pvpInflado)));
                                    }
                                    break;

                                case DIVISOR:
                                    if (valor.compareTo(BigDecimal.ZERO) > 0) {
                                        pvpInflado = pvpInflado.divide(valor, 6, RoundingMode.HALF_UP);
                                        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                                                "Inflar precio (divisor)",
                                                "PVP = PVP / VALOR",
                                                pvpInflado,
                                                String.format("Divisor: %s → %s / %s = %s",
                                                        valor, pvpAntesPromo, valor, pvpInflado)));
                                    }
                                    break;

                                case PRECIO_FIJO:
                                    if (valor.compareTo(BigDecimal.ZERO) > 0) {
                                        pvpInflado = valor;
                                        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                                                "Establecer precio fijo",
                                                "PVP = VALOR",
                                                pvpInflado,
                                                String.format("Precio fijo: %s", valor)));
                                    }
                                    break;

                                default:
                                    break;
                            }
                        }
                    }
                }
            }
        }

        pvpInflado = pvpInflado.setScale(2, RoundingMode.HALF_UP);

        // Verificar que el resultado final coincida con el cálculo real
        // Usar el mismo método aplicarPromocion para asegurar consistencia
        BigDecimal pvpVerificado = aplicarPromocion(producto.getId(), productoCanal.getCanal(), pvpSinPromocion);
        pvpVerificado = pvpVerificado.setScale(2, RoundingMode.HALF_UP);

        // Si hay diferencia, usar el valor verificado
        if (pvpVerificado.compareTo(pvpInflado) != 0) {
            pvpInflado = pvpVerificado;
        }

        // Construir fórmula general usando nombres definidos en los pasos anteriores
        StringBuilder formulaGeneralBuilder = new StringBuilder("PVP = (((COSTO");

        // Agregar gastos sobre COSTO_MARGEN si existen
        List<String> nombresConceptosCostoMargenFinal = obtenerNombresConceptos(conceptos, AplicaSobre.COSTO_MARGEN);
        String nombresCostoMargenFinalFormateados = formatearNombresConceptos(nombresConceptosCostoMargenFinal);
        if (!nombresCostoMargenFinalFormateados.isEmpty()) {
            formulaGeneralBuilder.append(" + ").append(nombresCostoMargenFinalFormateados);
        }

        formulaGeneralBuilder.append(") * IMP");

        // Agregar gastos sobre COSTO_IVA si existen
        List<String> nombresConceptosCostoIvaFinal = obtenerNombresConceptos(conceptos, AplicaSobre.COSTO_IVA);
        String nombresCostoIvaFinalFormateados = formatearNombresConceptos(nombresConceptosCostoIvaFinal);
        if (!nombresCostoIvaFinalFormateados.isEmpty()) {
            formulaGeneralBuilder.append(" * ").append(nombresCostoIvaFinalFormateados);
        }

        formulaGeneralBuilder.append(") / (1 - ").append(nombreGastosPVP).append("/100)");

        // Agregar GTML si aplica cuotas
        if (productoCanal.getAplicaCuotas() != null && productoCanal.getAplicaCuotas() && numeroCuotas != null) {
            formulaGeneralBuilder.append(" / (1 - GTML/100)");
        }

        // Agregar DESCUENTO si existe
        if (descuentoTotal.compareTo(BigDecimal.ZERO) > 0) {
            formulaGeneralBuilder.append(" / (1 - ").append(nombreDescuento).append("/100)");
        }

        // Agregar MARGEN_FIJO si existe
        if (margenFijo.compareTo(BigDecimal.ZERO) > 0) {
            formulaGeneralBuilder.append(" + ").append(nombreMargenFijo);
        }

        // Agregar PROMOCIONES si existen
        boolean tienePromociones = (canal != null && canal.getPorcentajeInflacion() != null
                && canal.getPorcentajeInflacion().compareTo(BigDecimal.ZERO) > 0)
                || (canal != null && productoCanalPromocionRepository
                        .findByProductoIdAndCanalId(producto.getId(), canal.getId()).isPresent());
        if (tienePromociones) {
            formulaGeneralBuilder.append(" + PROMOCIONES");
        }

        String formulaGeneral = formulaGeneralBuilder.toString();

        return new FormulaCalculoDTO(
                productoCanal.getCanal().getCanal(),
                numeroCuotas,
                formulaGeneral,
                pasos,
                pvpInflado);
    }
}