package ar.com.leo.super_master_backend.dominio.producto.calculo.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.leo.super_master_backend.dominio.canal.entity.Canal;
import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConcepto;
import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConceptoCuota;
import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConceptoRegla;
import ar.com.leo.super_master_backend.dominio.canal.entity.TipoCuota;
import ar.com.leo.super_master_backend.dominio.canal.entity.TipoRegla;
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
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanal;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanalPrecio;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanalPromocion;
import ar.com.leo.super_master_backend.dominio.producto.mla.repository.MlaRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalPrecioRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalPromocionRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCatalogoRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoRepository;
import ar.com.leo.super_master_backend.dominio.promocion.entity.Promocion;
import ar.com.leo.super_master_backend.dominio.promocion.entity.TipoPromocionTabla;
import ar.com.leo.super_master_backend.dominio.regla_descuento.entity.ReglaDescuento;
import ar.com.leo.super_master_backend.dominio.regla_descuento.repository.ReglaDescuentoRepository;
import lombok.RequiredArgsConstructor;

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
     * Fórmula principal del Excel para ML: PVP ML = ((([@[ML-COSTO+GAN]] +
     * [@ENVIO]) * [@[IMP.]]) / ([@[%CUOTAS]])) / 0,9
     *
     * Donde: - ML-COSTO+GAN = [@COSTO] + ([@COSTO] * [@[GAN.MIN.ML]]) - @COSTO
     * = costo del producto (de DUX, tabla productos.costo) - @ENVIO = precio de
     * envío de ML (de tabla mlas.precio_envio) - @[GAN.MIN.ML] = % de ganancia
     * mínima del producto (de producto_canal.margen_porcentaje) - @[IMP.] = 1 +
     * [@IVA] + sum(conceptos con aplica_sobre='IMP') - @IVA = IVA del producto
     * (de DUX, tabla productos.iva, como porcentaje, ej: 21) - conceptos IMP =
     * conceptos de conceptos_gastos con aplica_sobre='IMP' (ej: IIBB = 5%) -
     *
     * @[%CUOTAS] = (1 - BUSCARX([@CUOTAS]; GTML[CUOTAS]; GTML[%])) - GTML[%] =
     * suma de porcentajes de conceptos_gastos con campo cuotas = número de
     * cuotas - Se obtiene de conceptos_gastos filtrando por id_canal=ML,
     * cuotas="3"/"6"/"9"/"12", etc. - 0,9 = gasto fijo del 10% para ML
     * (equivalente a dividir por 0.9)
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

        List<CanalConcepto> gastosSobreAumentaMargen = conceptos.stream()
                .filter(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.AUMENTA_MARGEN)
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

        // 6) Calcular ganancia ajustada con AUMENTA_MARGEN
        // Base: GAN.MIN.ML
        BigDecimal gananciaAjustada = margenPorcentaje;

        // Si hay AUMENTA_MARGEN: GANANCIA_AJUSTADA += AUMENTA_MARGEN (suma directa)
        BigDecimal aumentaMargenTotal = calcularGastosPorcentaje(gastosSobreAumentaMargen);
        if (aumentaMargenTotal.compareTo(BigDecimal.ZERO) > 0) {
            gananciaAjustada = gananciaAjustada.add(aumentaMargenTotal);
        }

        // 7) Calcular costo con ganancia ajustada
        BigDecimal gananciaFrac = gananciaAjustada.divide(cien, 6, RoundingMode.HALF_UP);
        BigDecimal costoConGananciaFinal = costoConGastos.multiply(BigDecimal.ONE.add(gananciaFrac));

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
        BigDecimal aumentaMargenTotalParaGastos = calcularGastosPorcentaje(gastosSobreAumentaMargen);
        BigDecimal gastosTotalPorcentaje = gastosSobreCostoTotal
                .add(aumentaMargenTotalParaGastos)
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
     * Determina si un producto es máquina basándose en la clasificación
     * gastronómica. Un producto es máquina si su clasifGastro tiene es_maquina
     * = true.
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
     * Calcula el precio para el canal KT HOGAR usando la fórmula especial del
     * Excel.
     *
     * Fórmula: PVP NUBE = ( ( ([@COSTO]+[@COSTO]*( SI( [@[GAN.MIN.ML]]=0;
     * "MARGEN"; [@[GAN.MIN.ML]] + 0,25 ) )) * [@[IMP.]] ) / (1-GTN) ) /
     * (1-CUPON)
     *
     * Donde: - Si GAN.MIN.ML = 0, usar MARGEN (margen base del canal) - Si
     * GAN.MIN.ML != 0, usar GAN.MIN.ML + 0.25 - @[IMP.] = 1 + [@IVA] + IIBB -
     * GTN (NUBE MENAJE X CUOTAS) = MP + NUBE + X CUOTAS + MARKETING + EMBALAJE
     * (donde X es el número de cuotas especificado) - CUPON se obtiene de
     * conceptos_gastos (si existe)
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
        // SI( [@[GAN.MIN.ML]]=0; "MARGEN"; [@[GAN.MIN.ML]] + AUMENTA_MARGEN )
        // El AUMENTA_MARGEN viene de un concepto de gasto con aplica_sobre='AUMENTA_MARGEN' asociado al canal
        // El valor en conceptos_gastos se guarda como porcentaje (ej: 25 = 25%)
        // Se suma directamente: GAN.MIN.ML + AUMENTA_MARGEN
        // NOTA: Los conceptos ya vienen filtrados por reglas de canal_concepto_regla desde obtenerConceptosAplicables
        BigDecimal margenPorcentaje = productoCanal.getMargenPorcentaje();
        if (margenPorcentaje == null) {
            margenPorcentaje = BigDecimal.ZERO;
        }

        // Filtrar conceptos con aplica_sobre='AUMENTA_MARGEN'
        List<CanalConcepto> gastosSobreAumentaMargen = conceptos.stream()
                .filter(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.AUMENTA_MARGEN)
                .collect(Collectors.toList());

        BigDecimal aumentaMargenTotal = calcularGastosPorcentaje(gastosSobreAumentaMargen);

        BigDecimal gananciaUsar;
        if (margenPorcentaje.compareTo(BigDecimal.ZERO) == 0) {
            // Si GAN.MIN.ML = 0, usar MARGEN (margen base del canal)
            // Por ahora, usamos 0 si no hay margen (esto debería revisarse según la lógica de negocio)
            gananciaUsar = BigDecimal.ZERO;
        } else {
            // Si GAN.MIN.ML != 0, calcular ganancia ajustada: GAN.MIN.ML + AUMENTA_MARGEN
            // Ejemplo: si margen = 60% y AUMENTA_MARGEN = 25%, entonces ganancia = 60% + 25% = 85%
            BigDecimal gananciaAjustada = margenPorcentaje;
            if (aumentaMargenTotal.compareTo(BigDecimal.ZERO) > 0) {
                gananciaAjustada = gananciaAjustada.add(aumentaMargenTotal);
            }
            gananciaUsar = gananciaAjustada;
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
     * Calcula el precio para el canal KT GASTRO usando la fórmula especial del
     * Excel.
     *
     * Fórmula: PVP GASTRO = SI( [@TAG]="MAQUINA"; ( [@[PVP ML]] * (1-0,145) );
     * ( ( [@COSTO] * (1+([@[GAN.MIN.ML]] * (1-REL_ML_KTG))) * [@[IMP.]] ) /
     * (1-(MARKETING+EMBALAJE+GASTONUBE)) ) )
     *
     * Donde: - Si es máquina: PVP GASTRO = PVP ML * (1 - 0.145) - Si NO es
     * máquina: PVP GASTRO = (COSTO * (1 + (GAN.MIN.ML * (1 - REL_ML_KTG))) * IMP) / (1 - (MARKETING
     * + EMBALAJE + GASTONUBE)) - [@TAG]="MAQUINA" se determina por
     * clasif_gastro.es_maquina = true - [@[PVP ML]] = precio calculado para el
     * canal ML - [@[GAN.MIN.ML]] = margen_porcentaje del producto_canal para KT
     * GASTRO - REL_ML_KTG = concepto con aplica_sobre='REDUCE_MARGEN' que reduce la ganancia
     * - MARKETING, EMBALAJE, GASTONUBE = conceptos con aplica_sobre='PVP' del canal KT GASTRO
     */
    /**
     * Calcula el precio para el canal KT GASTRO usando fórmula unificada.
     * 
     * Fórmula unificada: PVP = (COSTO * (1 + GANANCIA_AJUSTADA) * IMP) / (1 - GASTOS_PVP) * (1 - DESCUENTO)
     * 
     * Donde:
     * - GANANCIA_AJUSTADA se calcula dinámicamente:
     *   - Base: GAN.MIN.ML (del canal KT GASTRO)
     *   - Si hay AUMENTA_MARGEN: GANANCIA_AJUSTADA += AUMENTA_MARGEN (suma directa de puntos porcentuales)
     *   - Si hay REDUCE_MARGEN: GANANCIA_AJUSTADA -= REDUCE_MARGEN (resta directa de puntos porcentuales)
     *   NOTA: Esta aplicación es consistente para todos los canales
     * - GASTOS_PVP: Suma de conceptos con aplica_sobre='PVP' (ya filtrados por reglas)
     * - %CUOTAS: Se incluye en GASTOS_PVP si existe (ya filtrado por reglas)
     * - DESCUENTO: Suma de conceptos con aplica_sobre='DESCUENTO' (ya filtrados por reglas)
     * 
     * Los conceptos ya vienen filtrados por reglas EXCLUIR desde obtenerConceptosAplicables(),
     * por lo que la fórmula se aplica igual para máquinas y no máquinas, diferenciándose
     * solo por los conceptos que están incluidos/excluidos según las reglas.
     */
    private PrecioCalculadoDTO calcularPrecioKTGastro(
            Producto producto,
            ProductoCanal productoCanal,
            List<CanalConcepto> conceptos,
            Integer numeroCuotas) {
        // Usar fórmula unificada (los conceptos ya vienen filtrados por reglas según máquina/no máquina)
        return calcularPrecioKTGastroUnificado(producto, productoCanal, conceptos, numeroCuotas);
    }

    /**
     * Calcula el precio para KT GASTRO usando fórmula unificada (aplica a máquinas y no máquinas).
     * Fórmula: PVP = (COSTO * (1 + GANANCIA_AJUSTADA) * IMP) / (1 - GASTOS_PVP) * (1 - DESCUENTO)
     * 
     * Donde:
     * - GANANCIA_AJUSTADA se calcula dinámicamente:
     *   - Base: GAN.MIN.ML (del canal KT GASTRO)
     *   - Si hay AUMENTA_MARGEN: GANANCIA_AJUSTADA += AUMENTA_MARGEN (suma directa de puntos porcentuales)
     *   - Si hay REDUCE_MARGEN: GANANCIA_AJUSTADA -= REDUCE_MARGEN (resta directa de puntos porcentuales)
     *   NOTA: Esta aplicación es consistente para todos los canales
     * - GASTOS_PVP: Suma de conceptos con aplica_sobre='PVP' (ya filtrados por reglas)
     * - %CUOTAS: Se incluye en GASTOS_PVP si existe (ya filtrado por reglas)
     * - DESCUENTO: Suma de conceptos con aplica_sobre='DESCUENTO' (ya filtrados por reglas)
     * 
     * NOTA: Los conceptos ya vienen filtrados por reglas EXCLUIR desde obtenerConceptosAplicables().
     * Para máquinas: incluye conceptos con regla EXCLUIR (es_maquina=false)
     * Para no máquinas: incluye conceptos con regla EXCLUIR (es_maquina=true)
     * El código busca dinámicamente los conceptos necesarios sin hardcodear qué buscar.
     * KT GASTRO usa sus propios conceptos, no depende del canal ML.
     */
    private PrecioCalculadoDTO calcularPrecioKTGastroUnificado(
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

        // 2) Obtener GAN.MIN.ML del canal KT GASTRO (base)
        BigDecimal gananciaMinML = BigDecimal.ZERO;
        if (productoCanal != null && productoCanal.getMargenPorcentaje() != null) {
            gananciaMinML = productoCanal.getMargenPorcentaje();
        }

        // 3) Obtener AUMENTA_MARGEN (conceptos ya filtrados por reglas)
        List<CanalConcepto> conceptosAumentaMargen = conceptos.stream()
                .filter(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.AUMENTA_MARGEN)
                .collect(Collectors.toList());

        BigDecimal aumentaMargen = calcularGastosPorcentaje(conceptosAumentaMargen);

        // 4) Obtener REDUCE_MARGEN (conceptos ya filtrados por reglas)
        List<CanalConcepto> conceptosReduceMargen = conceptos.stream()
                .filter(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.REDUCE_MARGEN)
                .collect(Collectors.toList());

        BigDecimal reduceMargen = calcularGastosPorcentaje(conceptosReduceMargen);

        // 5) Calcular GANANCIA_AJUSTADA
        // Inicio: GANANCIA_AJUSTADA = GAN.MIN.ML
        BigDecimal gananciaAjustada = gananciaMinML;

        // Si hay AUMENTA_MARGEN: GANANCIA_AJUSTADA += AUMENTA_MARGEN (suma directa)
        if (aumentaMargen.compareTo(BigDecimal.ZERO) > 0) {
            gananciaAjustada = gananciaAjustada.add(aumentaMargen);
        }

        // Si hay REDUCE_MARGEN: GANANCIA_AJUSTADA -= REDUCE_MARGEN (resta directa)
        if (reduceMargen.compareTo(BigDecimal.ZERO) > 0) {
            gananciaAjustada = gananciaAjustada.subtract(reduceMargen);
        }

        // 6) Calcular: COSTO * (1 + GANANCIA_AJUSTADA/100)
        BigDecimal gananciaFrac = gananciaAjustada.divide(cien, 6, RoundingMode.HALF_UP);
        BigDecimal costoConGanancia = costo.multiply(BigDecimal.ONE.add(gananciaFrac));

        // 7) Aplicar impuestos: COSTO_CON_GANANCIA * IMP
        BigDecimal costoConImpuestos = costoConGanancia.multiply(imp);

        // 8) Obtener gastos PVP (ya filtrados por reglas)
        List<CanalConcepto> gastosSobrePVP = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.PVP)
                .collect(Collectors.toList());

        BigDecimal gastosSobrePVPTotal = calcularGastosPorcentaje(gastosSobrePVP);

        // 9) Obtener %CUOTAS del canal KT GASTRO (si hay cuotas y está en los conceptos filtrados)
        BigDecimal porcentajeCuota = BigDecimal.ZERO;
        if (numeroCuotas != null && numeroCuotas > 0) {
            Integer idCanalKTGastro = productoCanal.getCanal().getId();
            List<CanalConceptoCuota> cuotasCanal = canalConceptoCuotaRepository.findByCanalIdAndCuotas(idCanalKTGastro,
                    numeroCuotas);
            Canal canalKTGastro = canalRepository.findById(idCanalKTGastro).orElse(null);
            if (canalKTGastro != null && canalKTGastro.getCanalBase() != null) {
                cuotasCanal.addAll(canalConceptoCuotaRepository
                        .findByCanalIdAndCuotas(canalKTGastro.getCanalBase().getId(), numeroCuotas));
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

        // 10) Calcular denominador: 1 - (GASTOS_PVP + %CUOTAS)/100
        BigDecimal gastosTotalPVP = gastosSobrePVPTotal.add(porcentajeCuota);
        BigDecimal gastosTotalPVPFrac = gastosTotalPVP.divide(cien, 6, RoundingMode.HALF_UP);
        BigDecimal denominador = BigDecimal.ONE.subtract(gastosTotalPVPFrac);
        if (denominador.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Los gastos sobre PVP son >= 100%, lo cual es inválido");
        }

        // 11) Calcular PVP: COSTO_CON_IMPUESTOS / (1 - GASTOS_PVP/100)
        BigDecimal pvpAntesDescuento = costoConImpuestos.divide(denominador, 6, RoundingMode.HALF_UP);

        // 12) Obtener descuentos (conceptos con aplica_sobre='DESCUENTO', ya filtrados por reglas)
        BigDecimal descuentoTotal = obtenerDescuentoMaquina(conceptos);

        // 13) Aplicar descuentos: PVP *= (1 - DESCUENTO/100)
        BigDecimal factorDescuento = BigDecimal.ONE.subtract(descuentoTotal.divide(cien, 6, RoundingMode.HALF_UP));
        BigDecimal pvp = pvpAntesDescuento.multiply(factorDescuento);
        BigDecimal pvpSinPromocion = pvp.setScale(2, RoundingMode.HALF_UP);

        // 14) Aplicar promociones
        BigDecimal pvpInflado = aplicarPromocion(producto.getId(), productoCanal.getCanal(), pvpSinPromocion);
        pvpInflado = pvpInflado.setScale(2, RoundingMode.HALF_UP);

        // 15) Calcular ganancia
        BigDecimal costoTotal = costo.setScale(2, RoundingMode.HALF_UP);
        BigDecimal gananciaAbs = costoConGanancia.subtract(costo).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gananciaPorcentaje = gananciaAbs
                .multiply(cien)
                .divide(costo, 2, RoundingMode.HALF_UP);

        // 16) Calcular gastos totales porcentaje
        BigDecimal gastosTotalPorcentaje = gastosSobreImpTotal.add(gastosSobrePVPTotal).add(porcentajeCuota);

        return new PrecioCalculadoDTO(
                pvpSinPromocion,
                pvpInflado,
                costoTotal,
                gananciaAbs,
                gananciaPorcentaje,
                gastosTotalPorcentaje);
    }

    /**
     * Calcula el precio para KT GASTRO cuando el producto es máquina. Fórmula:
     * ( ( (COSTO * (1 + GAN.MIN.ML)) * IMP) / (1 - COMISION ML + %CUOTAS +
     * MARKETING + EMBALAJE) ) * (1 - DESCUENTO_MAQUINA)
     *
     * NOTA: Los conceptos en el parámetro 'conceptos' ya vienen filtrados por
     * reglas EXCLUIR desde obtenerConceptosAplicables(). Para productos
     * máquinas, los conceptos con regla EXCLUIR (es_maquina=false) ya fueron
     * excluidos de la lista. Este método busca dinámicamente conceptos con los
     * aplica_sobre necesarios para la fórmula (IMP, PVP, DESCUENTO) sin
     * hardcodear qué conceptos buscar. Si no existen conceptos con un
     * aplica_sobre específico, retorna cero.
     * 
     * @deprecated Usar calcularPrecioKTGastroUnificado en su lugar
     */
    @Deprecated
    private PrecioCalculadoDTO calcularPrecioKTGastroMaquina(
            Producto producto,
            ProductoCanal productoCanalKTGastro,
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

        // 1) Obtener GAN.MIN.ML del canal ML
        // GAN.MIN.ML es el margen_porcentaje de la tabla producto_canal para el canal ML
        Canal canalML = canalRepository.findByCanalIgnoreCase("ML")
                .orElseThrow(() -> new NotFoundException("No se encontró el canal ML"));

        ProductoCanal productoCanalML = productoCanalRepository
                .findByProductoIdAndCanalId(producto.getId(), canalML.getId())
                .orElse(null);

        BigDecimal gananciaMinML = BigDecimal.ZERO;
        if (productoCanalML != null && productoCanalML.getMargenPorcentaje() != null) {
            gananciaMinML = productoCanalML.getMargenPorcentaje();
        }

        // 2) Calcular COSTO * (1 + GAN.MIN.ML)
        BigDecimal gananciaFrac = gananciaMinML.divide(cien, 6, RoundingMode.HALF_UP);
        BigDecimal costoConGanancia = costo.multiply(BigDecimal.ONE.add(gananciaFrac));

        // 3) Obtener IMP de conceptos con aplica_sobre='IMP' del canal KT GASTRO
        List<CanalConcepto> gastosSobreImp = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.IMP)
                .collect(Collectors.toList());

        BigDecimal gastosSobreImpTotal = calcularGastosPorcentaje(gastosSobreImp);
        BigDecimal imp = BigDecimal.ONE
                .add(iva.divide(cien, 6, RoundingMode.HALF_UP))
                .add(gastosSobreImpTotal.divide(cien, 6, RoundingMode.HALF_UP));

        // 4) Aplicar impuestos: costoConGanancia * IMP
        BigDecimal costoConImpuestos = costoConGanancia.multiply(imp);

        // 5) Obtener TODOS los conceptos PVP del canal KT GASTRO (COMISION ML + MARKETING + EMBALAJE)
        List<CanalConcepto> gastosSobrePVP = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.PVP)
                .collect(Collectors.toList());

        BigDecimal gastosSobrePVPTotal = calcularGastosPorcentaje(gastosSobrePVP);

        // 6) Obtener %CUOTAS del canal KT GASTRO (si hay cuotas)
        BigDecimal porcentajeCuota = BigDecimal.ZERO;
        if (numeroCuotas != null && numeroCuotas > 0) {
            Integer idCanalKTGastro = productoCanalKTGastro.getCanal().getId();
            List<CanalConceptoCuota> cuotasCanal = canalConceptoCuotaRepository.findByCanalIdAndCuotas(idCanalKTGastro,
                    numeroCuotas);
            Canal canalKTGastro = canalRepository.findById(idCanalKTGastro).orElse(null);
            if (canalKTGastro != null && canalKTGastro.getCanalBase() != null) {
                cuotasCanal.addAll(canalConceptoCuotaRepository
                        .findByCanalIdAndCuotas(canalKTGastro.getCanalBase().getId(), numeroCuotas));
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

        // 7) Calcular denominador: 1 - (suma_conceptos_PVP + %CUOTAS)/100
        BigDecimal gastosTotalPVP = gastosSobrePVPTotal.add(porcentajeCuota);
        BigDecimal gastosTotalPVPFrac = gastosTotalPVP.divide(cien, 6, RoundingMode.HALF_UP);
        BigDecimal denominador = BigDecimal.ONE.subtract(gastosTotalPVPFrac);
        if (denominador.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Los gastos sobre PVP son >= 100%, lo cual es inválido");
        }

        // 8) Dividir: costoConImpuestos / denominador
        BigDecimal pvpAntesDescuento = costoConImpuestos.divide(denominador, 6, RoundingMode.HALF_UP);

        // 9) Obtener conceptos con aplica_sobre='DESCUENTO' de la lista ya filtrada por reglas EXCLUIR
        // Los conceptos ya vienen filtrados: los conceptos con regla EXCLUIR (es_maquina=false) ya fueron excluidos
        // Este método busca dinámicamente cualquier concepto con aplica_sobre='DESCUENTO' en la lista filtrada
        // Si no existe (porque fue excluido), retorna cero (comportamiento correcto)
        BigDecimal descuentoMaquina = obtenerDescuentoMaquina(conceptos);

        // 10) Multiplicar por (1 - DESCUENTO_MAQUINA/100)
        BigDecimal factorDescuento = BigDecimal.ONE.subtract(descuentoMaquina.divide(cien, 6, RoundingMode.HALF_UP));
        BigDecimal pvp = pvpAntesDescuento.multiply(factorDescuento);
        BigDecimal pvpSinPromocion = pvp.setScale(2, RoundingMode.HALF_UP);

        // 11) Aplicar promociones del canal KT GASTRO
        BigDecimal pvpInflado = aplicarPromocion(producto.getId(), productoCanalKTGastro.getCanal(), pvpSinPromocion);
        pvpInflado = pvpInflado.setScale(2, RoundingMode.HALF_UP);

        // 12) Calcular ganancia
        BigDecimal costoTotal = costo.setScale(2, RoundingMode.HALF_UP);
        BigDecimal gananciaAbs = costoConGanancia.subtract(costo).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gananciaPorcentaje = gananciaAbs
                .multiply(cien)
                .divide(costo, 2, RoundingMode.HALF_UP);

        // 13) Calcular gastos totales porcentaje
        BigDecimal gastosTotalPorcentaje = gastosSobreImpTotal.add(gastosSobrePVPTotal).add(porcentajeCuota);

        return new PrecioCalculadoDTO(
                pvpSinPromocion,
                pvpInflado,
                costoTotal,
                gananciaAbs,
                gananciaPorcentaje,
                gastosTotalPorcentaje);
    }

    /**
     * Calcula el precio para KT GASTRO cuando el producto NO es máquina.
     * Fórmula: PVP GASTRO = (COSTO * (1 + (GAN.MIN.ML * (1 - REL_ML_KTG))) * IMP) / (1 - (MARKETING
     * + EMBALAJE + GASTONUBE))
     *
     * Donde:
     * - GAN.MIN.ML viene de producto_canal.margen_porcentaje del canal KT GASTRO
     * - REL_ML_KTG viene de conceptos con aplica_sobre='REDUCE_MARGEN' del canal KT GASTRO
     * - REL_ML_KTG reduce la ganancia multiplicando por (1 - REL_ML_KTG/100)
     * - Si no existe REL_ML_KTG (no hay conceptos REDUCE_MARGEN), REL_ML_KTG = 0, entonces GANANCIA_AJUSTADA = GAN.MIN.ML
     *
     * NOTA: Los conceptos en el parámetro 'conceptos' ya vienen filtrados por
     * reglas EXCLUIR desde obtenerConceptosAplicables(). Para productos no
     * máquinas, los conceptos con regla EXCLUIR (es_maquina=true) ya fueron
     * excluidos de la lista. Este método busca dinámicamente conceptos con los
     * aplica_sobre necesarios para la fórmula (IMP, PVP, REDUCE_MARGEN) sin hardcodear qué
     * conceptos buscar. Si no existen conceptos con un aplica_sobre específico,
     * retorna cero.
     * 
     * @deprecated Usar calcularPrecioKTGastroUnificado en su lugar
     */
    @Deprecated
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

        // 2) Obtener GAN.MIN.ML del canal KT GASTRO
        // GAN.MIN.ML es el margen_porcentaje de la tabla producto_canal para el canal KT GASTRO
        BigDecimal gananciaMinML = BigDecimal.ZERO;
        if (productoCanal != null && productoCanal.getMargenPorcentaje() != null) {
            gananciaMinML = productoCanal.getMargenPorcentaje();
        }

        // 2.5) Obtener REL_ML_KTG (concepto REDUCE_MARGEN que reduce la ganancia)
        // REL_ML_KTG viene de conceptos con aplica_sobre='REDUCE_MARGEN' del canal KT GASTRO
        // Los conceptos ya vienen filtrados por reglas EXCLUIR desde obtenerConceptosAplicables()
        List<CanalConcepto> conceptosRelMLKTG = conceptos.stream()
                .filter(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.REDUCE_MARGEN)
                .collect(Collectors.toList());

        BigDecimal relMLKTG = calcularGastosPorcentaje(conceptosRelMLKTG);

        // 3) Calcular ganancia ajustada: GAN.MIN.ML * (1 - REL_ML_KTG/100)
        // REL_ML_KTG reduce la ganancia multiplicando por (1 - REL_ML_KTG/100)
        BigDecimal factorRelacion = BigDecimal.ONE.subtract(relMLKTG.divide(cien, 6, RoundingMode.HALF_UP));
        BigDecimal gananciaAjustada = gananciaMinML.multiply(factorRelacion);

        // 4) Calcular: COSTO * (1 + GANANCIA_AJUSTADA/100)
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

        // 10) Calcular gastos totales porcentaje (incluyendo REL_ML_KTG si existe)
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
     * Obtiene el descuento de máquina desde los conceptos. Busca dinámicamente
     * conceptos con aplica_sobre='DESCUENTO' en la lista ya filtrada por reglas
     * EXCLUIR.
     *
     * NOTA: Los conceptos ya vienen filtrados por canal_concepto_regla desde
     * obtenerConceptosAplicables(). Para productos máquinas, los conceptos con
     * regla EXCLUIR (es_maquina=false) ya fueron excluidos de la lista. Este
     * método busca dinámicamente cualquier concepto con
     * aplica_sobre='DESCUENTO' en la lista filtrada, sin hardcodear nombres de
     * conceptos específicos. Si no existe (porque fue excluido), retorna cero.
     *
     * @param conceptos Lista de CanalConcepto ya filtrada por reglas EXCLUIR
     * (solo incluye conceptos aplicables)
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
     * @param canalId El ID del canal
     * @param pvpBase El PVP base antes de aplicar descuentos (para validar
     * monto mínimo)
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
        return productoRepository.findById(idProducto)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado"));
    }

    private ProductoCanal obtenerProductoCanal(Integer idProducto, Integer idCanal) {
        return productoCanalRepository
                .findByProductoIdAndCanalId(idProducto, idCanal)
                .orElseThrow(() -> new NotFoundException("No existe configuración de canal para este producto"));
    }

    /**
     * Obtiene todos los conceptos de gasto que aplican al canal según los
     * filtros.
     *
     * Sistema unificado: todos los conceptos se asocian a canales a través de
     * canal_concepto.
     *
     * Lógica de filtrado: - Canal: Un concepto aplica a un canal si está
     * asociado en canal_concepto - Jerarquía de canales: Si un concepto está
     * asignado al canal padre (ej: NUBE), también aplica a todos sus canales
     * hijos (ej: KT HOGAR, KT GASTRO) Nota: Las cuotas ahora se manejan a
     * través de canal_concepto_cuota, no a través del campo cuotas en
     * conceptos_gastos
     *
     * REGLAS DE CANAL_CONCEPTO_REGLA: - Si tipo_regla = INCLUIR: el concepto
     * SOLO aplica si el producto cumple TODAS las condiciones - Si tipo_regla =
     * EXCLUIR: el concepto NO aplica si el producto cumple ALGUNA condición -
     * Condiciones disponibles: id_tipo, id_clasif_gral, id_clasif_gastro,
     * id_marca, es_maquina - es_maquina: filtra por máquina/no máquina
     * (true=solo máquinas, false=solo no máquinas, NULL=no filtra)
     *
     * ENFOQUE RECOMENDADO PARA KT GASTRO (usar EXCLUIR):
     *
     * 1. Conceptos comunes (no requieren reglas): - MARKETING, EMBALAJE,
     * GASTONUBE, COMISION ML (aplica_sobre='PVP') - IIBB (aplica_sobre='IMP')
     * Estos conceptos aplican a todos los productos del canal.
     *
     * 2. DESCUENTO_MAQUINA (solo para máquinas): - Debe estar en canal_concepto
     * para KT GASTRO - Debe tener regla EXCLUIR con es_maquina=false (excluir
     * cuando NO es máquina) Ejemplo: tipo_regla='EXCLUIR', es_maquina=false
     * Resultado: Se excluye cuando producto.clasif_gastro.es_maquina = false →
     * Solo aplica a máquinas (es_maquina = true)
     *
     * NOTA: REL_ML_KTG se usa para KT GASTRO NO MÁQUINA como concepto con
     * aplica_sobre='REDUCE_MARGEN' que reduce la ganancia restando directamente.
     * Debe tener regla EXCLUIR con es_maquina=true para que solo aplique a no máquinas.
     *
     * NOTA: Los valores de aplica_sobre incluyen:
     * - AUMENTA_MARGEN: Suma puntos porcentuales directamente al margen (GAN.MIN.ML + AUMENTA_MARGEN)
     *                   Ejemplo: Si GAN.MIN.ML = 60% y AUMENTA_MARGEN = 25%, entonces ganancia = 60% + 25% = 85%
     * - REDUCE_MARGEN: Resta puntos porcentuales directamente del margen (GAN.MIN.ML - REDUCE_MARGEN)
     *                  Ejemplo: Si GAN.MIN.ML = 60% y REDUCE_MARGEN = 20%, entonces ganancia = 60% - 20% = 40%
     * - COSTO_MARGEN: Deprecado. Usar AUMENTA_MARGEN o REDUCE_MARGEN según corresponda.
     * NOTA: Esta aplicación es consistente para todos los canales.
     *
     * NOTA: Todos los conceptos (comunes y específicos) deben estar en
     * canal_concepto. Las reglas EXCLUIR filtran cuáles conceptos NO aplican
     * según las condiciones del producto. Los métodos de cálculo buscan
     * dinámicamente conceptos por aplica_sobre sin hardcodear qué buscar.
     *
     * @param idCanal ID del canal para filtrar conceptos
     * @param numeroCuotas Número de cuotas (parámetro mantenido por
     * compatibilidad, pero ya no se usa para filtrar conceptos)
     * @param esCanalNube true si el canal es KT HOGAR o KT GASTRO (canales
     * NUBE)
     * @param producto El producto para aplicar las reglas de
     * canal_concepto_regla
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
     * @param regla La regla a verificar
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
     * @param idCanal ID del canal para establecer en los objetos CanalConcepto
     * temporales
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
     * @param conceptos Lista de CanalConcepto
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
     * Aplica promociones al precio calculado: 1. Primero aplica
     * porcentaje_inflacion del canal (si existe) 2. Luego aplica la promoción
     * de producto_canal_promocion (si existe y está activa)
     *
     * @param productoId ID del producto
     * @param canal Canal con el porcentaje_inflacion
     * @param pvp Precio calculado antes de aplicar promociones
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
                .filter(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.AUMENTA_MARGEN)
                .collect(Collectors.toList());
        BigDecimal aumentaMargenTotal = calcularGastosPorcentaje(gastosSobreCostoMargen);
        BigDecimal gananciaUsar;
        if (margenPorcentaje.compareTo(BigDecimal.ZERO) == 0) {
            gananciaUsar = BigDecimal.ZERO;
        } else {
            // Calcular ganancia ajustada: MARGEN + AUMENTA_MARGEN (suma directa)
            gananciaUsar = margenPorcentaje.add(aumentaMargenTotal);
        }

        List<String> nombresConceptosAumentaMargen = obtenerNombresConceptos(conceptos, AplicaSobre.AUMENTA_MARGEN);
        String nombresAumentaMargenFormateados = formatearNombresConceptos(nombresConceptosAumentaMargen);
        String formulaGanancia = nombresAumentaMargenFormateados.isEmpty()
                ? "GANANCIA = MARGEN"
                : String.format("GANANCIA = MARGEN + %s", nombresAumentaMargenFormateados);

        String detalleGanancia = String.format("MARGEN: %s%%", margenPorcentaje);
        if (aumentaMargenTotal.compareTo(BigDecimal.ZERO) > 0 && !nombresAumentaMargenFormateados.isEmpty()) {
            detalleGanancia += String.format(" + %s", nombresAumentaMargenFormateados);
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
     * Fórmula: PVP GASTRO = SI( [@TAG]="MAQUINA"; ( [@[PVP ML]] * (1-0,145) );
     * ( ( [@COSTO] * (1+[@[GAN.MIN.ML]]) * [@[IMP.]] ) /
     * (1-(MARKETING+EMBALAJE+GASTONUBE)) ) )
     *
     * Donde GAN.MIN.ML se obtiene directamente de
     * producto_canal.margen_porcentaje del canal KT GASTRO.
     */
    /**
     * Genera la fórmula de cálculo para el canal KT GASTRO usando fórmula unificada.
     * 
     * Fórmula unificada: PVP = (COSTO * (1 + GANANCIA_AJUSTADA) * IMP) / (1 - GASTOS_PVP) * (1 - DESCUENTO)
     * 
     * Donde GANANCIA_AJUSTADA se calcula dinámicamente según los conceptos aplicables.
     */
    private FormulaCalculoDTO generarFormulaKTGastro(
            Producto producto,
            ProductoCanal productoCanal,
            List<CanalConcepto> conceptos,
            Integer numeroCuotas) {
        // Usar fórmula unificada (los conceptos ya vienen filtrados por reglas según máquina/no máquina)
        return generarFormulaKTGastroUnificado(producto, productoCanal, conceptos, numeroCuotas);
    }

    /**
     * Genera la fórmula unificada para KT GASTRO (aplica a máquinas y no máquinas).
     * Fórmula: PVP = (COSTO * (1 + GANANCIA_AJUSTADA) * IMP) / (1 - GASTOS_PVP) * (1 - DESCUENTO)
     */
    private FormulaCalculoDTO generarFormulaKTGastroUnificado(
            Producto producto,
            ProductoCanal productoCanal,
            List<CanalConcepto> conceptos,
            Integer numeroCuotas) {
        List<FormulaCalculoDTO.PasoCalculo> pasos = new java.util.ArrayList<>();
        BigDecimal cien = BigDecimal.valueOf(100);
        int pasoNumero = 1;

        // Paso 1: Costo
        BigDecimal costo = producto.getCosto();
        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                "Costo base del producto",
                "COSTO",
                costo,
                String.format("Costo: $%s", costo)));

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
        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                "Factor de impuestos (IMP)",
                formulaImp,
                imp,
                detalleImp));

        // Paso 3: Obtener GAN.MIN.ML del canal KT GASTRO
        BigDecimal gananciaMinML = BigDecimal.ZERO;
        if (productoCanal != null && productoCanal.getMargenPorcentaje() != null) {
            gananciaMinML = productoCanal.getMargenPorcentaje();
        }

        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                "Ganancia mínima (GAN.MIN.ML)",
                "GAN.MIN.ML",
                gananciaMinML,
                String.format("Margen porcentaje (producto_canal.margen_porcentaje) del canal KT GASTRO: %s%%",
                        gananciaMinML)));

        // Paso 4: Obtener AUMENTA_MARGEN
        List<CanalConcepto> conceptosAumentaMargen = conceptos.stream()
                .filter(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.AUMENTA_MARGEN)
                .collect(Collectors.toList());

        BigDecimal aumentaMargen = calcularGastosPorcentaje(conceptosAumentaMargen);
        List<String> nombresAumentaMargen = obtenerNombresConceptos(conceptos, AplicaSobre.AUMENTA_MARGEN);
        String nombresAumentaMargenFormateados = formatearNombresConceptos(nombresAumentaMargen);

        if (aumentaMargen.compareTo(BigDecimal.ZERO) > 0) {
            pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                    "Aumentar margen (AUMENTA_MARGEN)",
                    nombresAumentaMargenFormateados.isEmpty() ? "AUMENTA_MARGEN"
                            : String.format("AUMENTA_MARGEN = %s", nombresAumentaMargenFormateados),
                    aumentaMargen,
                    nombresAumentaMargenFormateados.isEmpty()
                            ? String.format("AUMENTA_MARGEN = %s%%", aumentaMargen)
                            : String.format("%s = %s%%", nombresAumentaMargenFormateados, aumentaMargen)));
        }

        // Paso 5: Obtener REDUCE_MARGEN
        List<CanalConcepto> conceptosReduceMargen = conceptos.stream()
                .filter(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.REDUCE_MARGEN)
                .collect(Collectors.toList());

        BigDecimal reduceMargen = calcularGastosPorcentaje(conceptosReduceMargen);
        List<String> nombresReduceMargen = obtenerNombresConceptos(conceptos, AplicaSobre.REDUCE_MARGEN);
        String nombresReduceMargenFormateados = formatearNombresConceptos(nombresReduceMargen);

        if (reduceMargen.compareTo(BigDecimal.ZERO) > 0) {
            pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                    "Reducir margen (REDUCE_MARGEN)",
                    nombresReduceMargenFormateados.isEmpty() ? "REDUCE_MARGEN"
                            : String.format("REDUCE_MARGEN = %s", nombresReduceMargenFormateados),
                    reduceMargen,
                    nombresReduceMargenFormateados.isEmpty()
                            ? String.format("REDUCE_MARGEN = %s%%", reduceMargen)
                            : String.format("%s = %s%%", nombresReduceMargenFormateados, reduceMargen)));
        }

        // Paso 6: Calcular ganancia ajustada
        BigDecimal gananciaAjustada = gananciaMinML;
        String detalleGananciaAjustada = String.format("GANANCIA_AJUSTADA = %s%%", gananciaMinML);

        if (aumentaMargen.compareTo(BigDecimal.ZERO) > 0) {
            gananciaAjustada = gananciaAjustada.add(aumentaMargen);
            detalleGananciaAjustada += String.format(" + %s = %s%%", aumentaMargen, gananciaAjustada);
        }

        if (reduceMargen.compareTo(BigDecimal.ZERO) > 0) {
            gananciaAjustada = gananciaAjustada.subtract(reduceMargen);
            detalleGananciaAjustada += String.format(" - %s = %s%%", reduceMargen, gananciaAjustada);
        }

        if (aumentaMargen.compareTo(BigDecimal.ZERO) > 0 || reduceMargen.compareTo(BigDecimal.ZERO) > 0) {
            pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                    "Ganancia ajustada",
                    "GANANCIA_AJUSTADA = GAN.MIN.ML" +
                            (aumentaMargen.compareTo(BigDecimal.ZERO) > 0 ? " + AUMENTA_MARGEN" : "") +
                            (reduceMargen.compareTo(BigDecimal.ZERO) > 0 ? " - REDUCE_MARGEN" : ""),
                    gananciaAjustada,
                    detalleGananciaAjustada));
        }

        // Paso 7: Calcular costo con ganancia
        BigDecimal gananciaFrac = gananciaAjustada.divide(cien, 6, RoundingMode.HALF_UP);
        BigDecimal costoConGanancia = costo.multiply(BigDecimal.ONE.add(gananciaFrac));

        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                "Costo con ganancia",
                "COSTO_CON_GANANCIA = COSTO * (1 + GANANCIA_AJUSTADA/100)",
                costoConGanancia,
                String.format("%s * (1 + %s/100) = %s * %s = %s",
                        costo, gananciaAjustada, costo, BigDecimal.ONE.add(gananciaFrac), costoConGanancia)));

        // Paso 8: Aplicar impuestos
        BigDecimal costoConImpuestos = costoConGanancia.multiply(imp);

        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                "Costo con impuestos",
                "COSTO_CON_IMPUESTOS = COSTO_CON_GANANCIA * IMP",
                costoConImpuestos,
                String.format("%s * %s = %s", costoConGanancia, imp, costoConImpuestos)));

        // Paso 9: Obtener gastos PVP
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
                "Gastos sobre PVP",
                formulaGastosPVP,
                gastosSobrePVPTotal,
                nombresPVPFormateados.isEmpty()
                        ? String.format("Gastos PVP: %s%%", gastosSobrePVPTotal)
                        : String.format("Gastos PVP (%s): %s%%", nombresPVPFormateados, gastosSobrePVPTotal)));

        // Paso 10: Obtener %CUOTAS
        BigDecimal porcentajeCuota = BigDecimal.ZERO;
        if (numeroCuotas != null && numeroCuotas > 0) {
            Integer idCanalKTGastro = productoCanal.getCanal().getId();
            List<CanalConceptoCuota> cuotasCanal = canalConceptoCuotaRepository.findByCanalIdAndCuotas(idCanalKTGastro,
                    numeroCuotas);
            Canal canalKTGastro = canalRepository.findById(idCanalKTGastro).orElse(null);
            if (canalKTGastro != null && canalKTGastro.getCanalBase() != null) {
                cuotasCanal.addAll(canalConceptoCuotaRepository
                        .findByCanalIdAndCuotas(canalKTGastro.getCanalBase().getId(), numeroCuotas));
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

        if (porcentajeCuota.compareTo(BigDecimal.ZERO) > 0) {
            pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                    "Porcentaje de cuotas",
                    String.format("%%CUOTAS = %s", numeroCuotas),
                    porcentajeCuota,
                    String.format("Porcentaje de cuotas (%s cuotas): %s%%", numeroCuotas, porcentajeCuota)));
        }

        // Paso 11: Calcular PVP
        BigDecimal gastosTotalPVP = gastosSobrePVPTotal.add(porcentajeCuota);
        BigDecimal gastosTotalPVPFrac = gastosTotalPVP.divide(cien, 6, RoundingMode.HALF_UP);
        BigDecimal denominador = BigDecimal.ONE.subtract(gastosTotalPVPFrac);
        BigDecimal pvpAntesDescuento = costoConImpuestos.divide(denominador, 6, RoundingMode.HALF_UP);

        String formulaPVP = nombresPVPFormateados.isEmpty() && porcentajeCuota.compareTo(BigDecimal.ZERO) == 0
                ? "PVP = COSTO_CON_IMPUESTOS / (1 - GASTOS_PVP/100)"
                : String.format("PVP = COSTO_CON_IMPUESTOS / (1 - (%s%s)/100)",
                        nombresPVPFormateados.isEmpty() ? "" : nombresPVPFormateados,
                        porcentajeCuota.compareTo(BigDecimal.ZERO) > 0
                                ? (nombresPVPFormateados.isEmpty() ? "" : " + ")
                                        + String.format("%s cuotas", numeroCuotas)
                                : "");

        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                "PVP antes de descuentos",
                formulaPVP,
                pvpAntesDescuento,
                String.format("%s / (1 - %s/100) = %s",
                        costoConImpuestos, gastosTotalPVP, pvpAntesDescuento)));

        // Paso 12: Obtener descuentos
        BigDecimal descuentoTotal = obtenerDescuentoMaquina(conceptos);
        List<String> nombresConceptosDescuento = obtenerNombresConceptos(conceptos, AplicaSobre.DESCUENTO);
        String nombresDescuentoFormateados = formatearNombresConceptos(nombresConceptosDescuento);

        if (descuentoTotal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal factorDescuento = BigDecimal.ONE.subtract(descuentoTotal.divide(cien, 6, RoundingMode.HALF_UP));
            BigDecimal pvp = pvpAntesDescuento.multiply(factorDescuento);
            BigDecimal pvpSinPromocion = pvp.setScale(2, RoundingMode.HALF_UP);

            pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                    "Aplicar descuentos",
                    nombresDescuentoFormateados.isEmpty() ? "PVP = PVP * (1 - DESCUENTO/100)"
                            : String.format("PVP = PVP * (1 - %s/100)", nombresDescuentoFormateados),
                    pvpSinPromocion,
                    String.format("%s * (1 - %s/100) = %s",
                            pvpAntesDescuento, descuentoTotal, pvpSinPromocion)));

            // Paso 13: Aplicar promociones
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
            StringBuilder formulaGeneralBuilder = new StringBuilder(
                    "PVP = (COSTO * (1 + GANANCIA_AJUSTADA/100) * IMP) / (1 - GASTOS_PVP/100) * (1 - DESCUENTO/100)");
            if (!nombresAumentaMargenFormateados.isEmpty() || !nombresReduceMargenFormateados.isEmpty()) {
                formulaGeneralBuilder.append(" donde GANANCIA_AJUSTADA = GAN.MIN.ML");
                if (!nombresAumentaMargenFormateados.isEmpty()) {
                    formulaGeneralBuilder.append(" * (1 + ").append(nombresAumentaMargenFormateados).append("/100)");
                }
                if (!nombresReduceMargenFormateados.isEmpty()) {
                    formulaGeneralBuilder.append(" * (1 - ").append(nombresReduceMargenFormateados).append("/100)");
                }
            }
            if (!nombresPVPFormateados.isEmpty()) {
                formulaGeneralBuilder.append(" y GASTOS_PVP = ").append(nombresPVPFormateados);
            }
            if (!nombresDescuentoFormateados.isEmpty()) {
                formulaGeneralBuilder.append(" y DESCUENTO = ").append(nombresDescuentoFormateados);
            }

            return new FormulaCalculoDTO(
                    productoCanal.getCanal().getCanal(),
                    numeroCuotas,
                    formulaGeneralBuilder.toString(),
                    pasos,
                    pvpInflado);
        } else {
            // Sin descuentos
            BigDecimal pvpSinPromocion = pvpAntesDescuento.setScale(2, RoundingMode.HALF_UP);

            // Paso 13: Aplicar promociones
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
            StringBuilder formulaGeneralBuilder = new StringBuilder(
                    "PVP = (COSTO * (1 + GANANCIA_AJUSTADA/100) * IMP) / (1 - GASTOS_PVP/100)");
            if (!nombresAumentaMargenFormateados.isEmpty() || !nombresReduceMargenFormateados.isEmpty()) {
                formulaGeneralBuilder.append(" donde GANANCIA_AJUSTADA = GAN.MIN.ML");
                if (!nombresAumentaMargenFormateados.isEmpty()) {
                    formulaGeneralBuilder.append(" * (1 + ").append(nombresAumentaMargenFormateados).append("/100)");
                }
                if (!nombresReduceMargenFormateados.isEmpty()) {
                    formulaGeneralBuilder.append(" * (1 - ").append(nombresReduceMargenFormateados).append("/100)");
                }
            }
            if (!nombresPVPFormateados.isEmpty()) {
                formulaGeneralBuilder.append(" y GASTOS_PVP = ").append(nombresPVPFormateados);
            }

            return new FormulaCalculoDTO(
                    productoCanal.getCanal().getCanal(),
                    numeroCuotas,
                    formulaGeneralBuilder.toString(),
                    pasos,
                    pvpInflado);
        }
    }

    /**
     * Genera la fórmula cuando el producto es máquina. Fórmula: ( ( (COSTO * (1
     * + GAN.MIN.ML)) * IMP) / (1 - COMISION ML + %CUOTAS + MARKETING +
     * EMBALAJE) ) * (1 - DESCUENTO_MAQUINA)
     * 
     * @deprecated Usar generarFormulaKTGastroUnificado en su lugar
     */
    @Deprecated
    private FormulaCalculoDTO generarFormulaKTGastroMaquina(
            Producto producto,
            ProductoCanal productoCanal,
            List<CanalConcepto> conceptos,
            Integer numeroCuotas) {
        List<FormulaCalculoDTO.PasoCalculo> pasos = new java.util.ArrayList<>();
        BigDecimal cien = BigDecimal.valueOf(100);
        int pasoNumero = 1;

        // Paso 1: Información sobre el producto
        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                "Producto es máquina",
                "TAG = MAQUINA",
                BigDecimal.ONE,
                "El producto pertenece a una clasificación gastronómica marcada como máquina"));

        // Paso 2: Costo
        BigDecimal costo = producto.getCosto();
        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                "Costo base del producto",
                "COSTO",
                costo,
                String.format("Costo: $%s", costo)));

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
                String.format("Margen porcentaje (producto_canal.margen_porcentaje) del canal ML: %s%%",
                        gananciaMinML)));

        // Paso 4: Calcular COSTO * (1 + GAN.MIN.ML)
        BigDecimal gananciaFrac = gananciaMinML.divide(cien, 6, RoundingMode.HALF_UP);
        BigDecimal costoConGanancia = costo.multiply(BigDecimal.ONE.add(gananciaFrac));
        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                "Costo con ganancia",
                "COSTO_CON_GANANCIA = COSTO * (1 + GAN.MIN.ML/100)",
                costoConGanancia,
                String.format("GAN.MIN.ML: %s%% → %s * (1 + %s/100) = %s",
                        gananciaMinML, costo, gananciaMinML, costoConGanancia)));

        // Paso 5: Factor de impuestos (IMP)
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
        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                "Factor de impuestos (IMP)",
                formulaImp,
                imp,
                detalleImp));

        // Paso 6: Aplicar impuestos: costoConGanancia * IMP
        BigDecimal costoConImpuestos = costoConGanancia.multiply(imp);
        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                "Costo con impuestos",
                "COSTO_CON_IMPUESTOS = COSTO_CON_GANANCIA * IMP",
                costoConImpuestos,
                String.format("%s * %s = %s", costoConGanancia, imp, costoConImpuestos)));

        // Paso 7: Obtener TODOS los conceptos PVP del canal KT GASTRO
        List<CanalConcepto> gastosSobrePVP = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.PVP)
                .collect(Collectors.toList());

        BigDecimal gastosSobrePVPTotal = calcularGastosPorcentaje(gastosSobrePVP);
        List<String> nombresConceptosPVP = obtenerNombresConceptos(conceptos, AplicaSobre.PVP);
        String nombresPVPFormateados = formatearNombresConceptos(nombresConceptosPVP);

        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                "Gastos sobre PVP",
                nombresPVPFormateados.isEmpty() ? "GASTOS_PVP" : nombresPVPFormateados,
                gastosSobrePVPTotal,
                String.format("Gastos PVP (%s): %s%%",
                        nombresPVPFormateados.isEmpty() ? "sin conceptos" : nombresPVPFormateados,
                        gastosSobrePVPTotal)));

        // Paso 8: Obtener %CUOTAS del canal KT GASTRO
        BigDecimal porcentajeCuota = BigDecimal.ZERO;
        if (numeroCuotas != null && numeroCuotas > 0) {
            Integer idCanalKTGastro = productoCanal.getCanal().getId();
            List<CanalConceptoCuota> cuotasCanal = canalConceptoCuotaRepository.findByCanalIdAndCuotas(idCanalKTGastro,
                    numeroCuotas);
            Canal canalKTGastro = canalRepository.findById(idCanalKTGastro).orElse(null);
            if (canalKTGastro != null && canalKTGastro.getCanalBase() != null) {
                cuotasCanal.addAll(canalConceptoCuotaRepository
                        .findByCanalIdAndCuotas(canalKTGastro.getCanalBase().getId(), numeroCuotas));
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

        if (porcentajeCuota.compareTo(BigDecimal.ZERO) > 0) {
            pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                    "Porcentaje de cuotas",
                    String.format("%s cuotas", numeroCuotas),
                    porcentajeCuota,
                    String.format("Porcentaje para %s cuotas: %s%%", numeroCuotas, porcentajeCuota)));
        }

        // Paso 9: Calcular denominador y dividir
        BigDecimal gastosTotalPVP = gastosSobrePVPTotal.add(porcentajeCuota);
        BigDecimal gastosTotalPVPFrac = gastosTotalPVP.divide(cien, 6, RoundingMode.HALF_UP);
        BigDecimal denominador = BigDecimal.ONE.subtract(gastosTotalPVPFrac);

        String formulaGastosTotal = nombresPVPFormateados.isEmpty()
                ? (porcentajeCuota.compareTo(BigDecimal.ZERO) > 0
                        ? String.format("%s cuotas", numeroCuotas)
                        : "0")
                : (porcentajeCuota.compareTo(BigDecimal.ZERO) > 0
                        ? String.format("%s + %s cuotas", nombresPVPFormateados, numeroCuotas)
                        : nombresPVPFormateados);

        BigDecimal pvpAntesDescuento = costoConImpuestos.divide(denominador, 6, RoundingMode.HALF_UP);
        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                "PVP antes de descuento máquina",
                String.format("PVP = COSTO_CON_IMPUESTOS / (1 - (%s)/100)", formulaGastosTotal),
                pvpAntesDescuento,
                String.format("%s / (1 - %s/100) = %s",
                        costoConImpuestos, gastosTotalPVP, pvpAntesDescuento)));

        // Paso 10: Obtener DESCUENTO_MAQUINA
        BigDecimal descuentoMaquina = obtenerDescuentoMaquina(conceptos);
        List<String> nombresConceptosDescuento = obtenerNombresConceptos(conceptos, AplicaSobre.DESCUENTO);
        String nombresDescuentoFormateados = formatearNombresConceptos(nombresConceptosDescuento);

        if (descuentoMaquina.compareTo(BigDecimal.ZERO) > 0) {
            pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                    "Descuento máquina",
                    nombresDescuentoFormateados.isEmpty() ? "DESCUENTO_MAQUINA" : nombresDescuentoFormateados,
                    descuentoMaquina,
                    String.format("Descuento máquina (%s): %s%%",
                            nombresDescuentoFormateados.isEmpty() ? "DESCUENTO" : nombresDescuentoFormateados,
                            descuentoMaquina)));

            // Paso 11: Aplicar descuento máquina
            BigDecimal factorDescuento = BigDecimal.ONE
                    .subtract(descuentoMaquina.divide(cien, 6, RoundingMode.HALF_UP));
            BigDecimal pvp = pvpAntesDescuento.multiply(factorDescuento);
            BigDecimal pvpSinPromocion = pvp.setScale(2, RoundingMode.HALF_UP);
            pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                    "PVP con descuento máquina",
                    String.format("PVP = PVP * (1 - %s/100)",
                            nombresDescuentoFormateados.isEmpty() ? "DESCUENTO_MAQUINA" : nombresDescuentoFormateados),
                    pvpSinPromocion,
                    String.format("%s * (1 - %s/100) = %s",
                            pvpAntesDescuento, descuentoMaquina, pvpSinPromocion)));
        } else {
            BigDecimal pvpSinPromocion = pvpAntesDescuento.setScale(2, RoundingMode.HALF_UP);
            pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                    "PVP sin descuento máquina",
                    "PVP = PVP_ANTES_DESCUENTO",
                    pvpSinPromocion,
                    String.format("Sin descuento máquina aplicable: %s", pvpSinPromocion)));
        }

        // Paso 12: Aplicar promociones del canal KT GASTRO
        BigDecimal pvpAntesPromo = pasoNumero > 1
                ? pasos.get(pasos.size() - 1).valor()
                : BigDecimal.ZERO;
        BigDecimal pvpInflado = aplicarPromocion(producto.getId(), productoCanal.getCanal(), pvpAntesPromo);
        pvpInflado = pvpInflado.setScale(2, RoundingMode.HALF_UP);

        if (pvpInflado.compareTo(pvpAntesPromo) != 0) {
            // Detallar promociones como en generarFormulaGeneral
            int pasoNumeroPromo = pasoNumero;
            Canal canal = productoCanal.getCanal();

            // Aplicar porcentaje_inflacion del canal
            if (canal.getPorcentajeInflacion() != null
                    && canal.getPorcentajeInflacion().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal porcentajeInflacion = canal.getPorcentajeInflacion();
                BigDecimal inflacionFrac = porcentajeInflacion.divide(cien, 6, RoundingMode.HALF_UP);
                BigDecimal divisorInflacion = BigDecimal.ONE.subtract(inflacionFrac);
                BigDecimal pvpConInflacion = pvpAntesPromo.divide(divisorInflacion, 6, RoundingMode.HALF_UP);
                pvpInflado = pvpConInflacion.setScale(2, RoundingMode.HALF_UP);

                pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumeroPromo++,
                        "Inflación del canal",
                        "PVP = PVP / (1 - PORCENTAJE_INFLACION/100)",
                        pvpInflado,
                        String.format("Inflación: %s%% → %s / (1 - %s/100) = %s",
                                porcentajeInflacion, pvpAntesPromo, porcentajeInflacion, pvpInflado)));
            }

            // Aplicar promociones de producto_canal_promocion
            Optional<ProductoCanalPromocion> promocionOpt = productoCanalPromocionRepository
                    .findByProductoIdAndCanalId(producto.getId(), canal.getId());

            if (promocionOpt.isPresent()) {
                ProductoCanalPromocion pcp = promocionOpt.get();
                if (pcp.getActiva() != null && pcp.getActiva()) {
                    Promocion promocion = pcp.getPromocion();
                    if (promocion != null) {
                        BigDecimal valor = promocion.getValor();
                        TipoPromocionTabla tipo = promocion.getTipo();

                        BigDecimal pvpAntesPromoActual = pvpInflado;

                        switch (tipo) {
                            case MULTIPLICADOR:
                                if (valor.compareTo(BigDecimal.ZERO) > 0) {
                                    pvpInflado = pvpInflado.multiply(valor);
                                    pvpInflado = pvpInflado.setScale(2, RoundingMode.HALF_UP);
                                    pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumeroPromo++,
                                            "Aplicar promoción multiplicador",
                                            "PVP = PVP * VALOR",
                                            pvpInflado,
                                            String.format("Multiplicador: %s → %s * %s = %s",
                                                    valor, pvpAntesPromoActual, valor, pvpInflado)));
                                }
                                break;

                            case DESCUENTO_PORC:
                                if (valor.compareTo(BigDecimal.ZERO) > 0 && valor.compareTo(cien) < 0) {
                                    BigDecimal promocionFrac = valor.divide(cien, 6, RoundingMode.HALF_UP);
                                    BigDecimal denominadorPromo = BigDecimal.ONE.subtract(promocionFrac);
                                    pvpInflado = pvpInflado.divide(denominadorPromo, 6, RoundingMode.HALF_UP);
                                    pvpInflado = pvpInflado.setScale(2, RoundingMode.HALF_UP);
                                    pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumeroPromo++,
                                            "Inflar precio (promoción porcentual)",
                                            "PVP = PVP / (1 - VALOR/100)",
                                            pvpInflado,
                                            String.format("Inflación: %s%% → %s / (1 - %s/100) = %s",
                                                    valor, pvpAntesPromoActual, valor, pvpInflado)));
                                }
                                break;

                            case DIVISOR:
                                if (valor.compareTo(BigDecimal.ZERO) > 0) {
                                    pvpInflado = pvpInflado.divide(valor, 6, RoundingMode.HALF_UP);
                                    pvpInflado = pvpInflado.setScale(2, RoundingMode.HALF_UP);
                                    pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumeroPromo++,
                                            "Inflar precio (divisor)",
                                            "PVP = PVP / VALOR",
                                            pvpInflado,
                                            String.format("Divisor: %s → %s / %s = %s",
                                                    valor, pvpAntesPromoActual, valor, pvpInflado)));
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

        pvpInflado = pvpInflado.setScale(2, RoundingMode.HALF_UP);

        // Construir fórmula general dinámicamente
        StringBuilder formulaGeneralBuilder = new StringBuilder("PVP = (((COSTO * (1 + GAN.MIN.ML)) * IMP");
        if (!nombresImpFormateados.isEmpty()) {
            formulaGeneralBuilder.append(" (").append(nombresImpFormateados).append(")");
        }
        formulaGeneralBuilder.append(") / (1 - (");
        if (!nombresPVPFormateados.isEmpty()) {
            formulaGeneralBuilder.append(nombresPVPFormateados);
            if (porcentajeCuota.compareTo(BigDecimal.ZERO) > 0) {
                formulaGeneralBuilder.append(" + %CUOTAS");
            }
        } else if (porcentajeCuota.compareTo(BigDecimal.ZERO) > 0) {
            formulaGeneralBuilder.append("%CUOTAS");
        } else {
            formulaGeneralBuilder.append("0");
        }
        formulaGeneralBuilder.append(")/100)");
        if (descuentoMaquina.compareTo(BigDecimal.ZERO) > 0) {
            formulaGeneralBuilder.append(" * (1 - ");
            if (!nombresDescuentoFormateados.isEmpty()) {
                formulaGeneralBuilder.append(nombresDescuentoFormateados);
            } else {
                formulaGeneralBuilder.append("DESCUENTO_MAQUINA");
            }
            formulaGeneralBuilder.append("/100)");
        }
        String formulaGeneral = formulaGeneralBuilder.toString();

        return new FormulaCalculoDTO(
                productoCanal.getCanal().getCanal(),
                numeroCuotas,
                formulaGeneral,
                pasos,
                pvpInflado);
    }

    /**
     * Genera la fórmula cuando el producto NO es máquina. Fórmula: PVP GASTRO =
     * (COSTO * (1 + (GAN.MIN.ML * (1 - REL_ML_KTG))) * IMP) / (1 - (MARKETING + EMBALAJE +
     * GASTONUBE))
     * 
     * @deprecated Usar generarFormulaKTGastroUnificado en su lugar
     */
    @Deprecated
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

        // Paso 3: Obtener GAN.MIN.ML del canal KT GASTRO
        // GAN.MIN.ML es el margen_porcentaje de la tabla producto_canal para el canal KT GASTRO (ya incluye el ajuste)
        BigDecimal gananciaMinML = BigDecimal.ZERO;
        if (productoCanal != null && productoCanal.getMargenPorcentaje() != null) {
            gananciaMinML = productoCanal.getMargenPorcentaje();
        }

        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                "Ganancia mínima (GAN.MIN.ML)",
                "GAN.MIN.ML",
                gananciaMinML,
                String.format("Margen porcentaje (producto_canal.margen_porcentaje) del canal KT GASTRO: %s%%",
                        gananciaMinML)));

        // Paso 4: Obtener REL_ML_KTG (conceptos REDUCE_MARGEN)
        List<CanalConcepto> conceptosRelMLKTG = conceptos.stream()
                .filter(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.REDUCE_MARGEN)
                .collect(Collectors.toList());

        BigDecimal relMLKTG = calcularGastosPorcentaje(conceptosRelMLKTG);
        List<String> nombresRelMLKTG = obtenerNombresConceptos(conceptos, AplicaSobre.REDUCE_MARGEN);
        String nombresRelMLKTGFormateados = formatearNombresConceptos(nombresRelMLKTG);

        pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++,
                "Relación ML-KT GASTRO (REL_ML_KTG)",
                nombresRelMLKTGFormateados.isEmpty() ? "REL_ML_KTG = 0"
                        : String.format("REL_ML_KTG = %s", nombresRelMLKTGFormateados),
                relMLKTG,
                nombresRelMLKTGFormateados.isEmpty()
                        ? String.format("REL_ML_KTG = %s%%", relMLKTG)
                        : String.format("%s = %s%%", nombresRelMLKTGFormateados, relMLKTG)));

        // Paso 5: Calcular ganancia ajustada: GAN.MIN.ML * (1 - REL_ML_KTG/100)
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
        StringBuilder formulaGeneralBuilder = new StringBuilder(
                "PVP = (COSTO * (1 + (GAN.MIN.ML * (1 - REL_ML_KTG/100))/100) * IMP) / (1 - GASTOS_PVP/100)");
        if (!nombresRelMLKTGFormateados.isEmpty()) {
            formulaGeneralBuilder.append(" donde REL_ML_KTG = ").append(nombresRelMLKTGFormateados);
        }
        if (!nombresPVPFormateados.isEmpty()) {
            if (!nombresRelMLKTGFormateados.isEmpty()) {
                formulaGeneralBuilder.append(" y GASTOS_PVP = ");
            } else {
                formulaGeneralBuilder.append(" donde GASTOS_PVP = ");
            }
            formulaGeneralBuilder.append(nombresPVPFormateados);
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

        // Paso 5: Gastos sobre AUMENTA_MARGEN
        List<CanalConcepto> gastosSobreAumentaMargen = conceptos.stream()
                .filter(cc -> cc.getConcepto() != null
                        && cc.getConcepto().getAplicaSobre() == AplicaSobre.AUMENTA_MARGEN)
                .collect(Collectors.toList());
        BigDecimal gastosSobreAumentaMargenTotal = calcularGastosPorcentaje(gastosSobreAumentaMargen);
        BigDecimal costoConGananciaFinal = costoConGanancia;
        if (gastosSobreAumentaMargenTotal.compareTo(BigDecimal.ZERO) > 0) {
            // Aplicar AUMENTA_MARGEN sumando directamente al margen
            BigDecimal gananciaAjustada = margenPorcentaje.add(gastosSobreAumentaMargenTotal);
            BigDecimal gananciaAjustadaFrac = gananciaAjustada.divide(cien, 6, RoundingMode.HALF_UP);
            costoConGananciaFinal = costoConGastos.multiply(BigDecimal.ONE.add(gananciaAjustadaFrac));

            List<String> nombresConceptosAumentaMargen = obtenerNombresConceptos(conceptos, AplicaSobre.AUMENTA_MARGEN);
            String nombresAumentaMargenFormateados = formatearNombresConceptos(nombresConceptosAumentaMargen);
            String nombreGastosAumentaMargen = nombresAumentaMargenFormateados.isEmpty() ? "AUMENTA_MARGEN"
                    : nombresAumentaMargenFormateados;
            String formulaAumentaMargen = String.format(
                    "COSTO_CON_GANANCIA_FINAL = COSTO_CON_GASTOS * (1 + GANANCIA_AJUSTADA/100) donde GANANCIA_AJUSTADA = MARGEN + %s",
                    nombreGastosAumentaMargen);
            pasos.add(new FormulaCalculoDTO.PasoCalculo(pasoNumero++, "Aumentar margen (AUMENTA_MARGEN)",
                    formulaAumentaMargen,
                    costoConGananciaFinal,
                    String.format(
                            "AUMENTA_MARGEN (%s): %s%% → GANANCIA_AJUSTADA = %s%% + %s%% = %s%% → %s * (1 + %s/100) = %s",
                            nombresAumentaMargenFormateados, gastosSobreAumentaMargenTotal, margenPorcentaje,
                            gastosSobreAumentaMargenTotal, gananciaAjustada, costoConGastos, gananciaAjustadaFrac,
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

        // Agregar gastos sobre AUMENTA_MARGEN si existen
        List<String> nombresConceptosAumentaMargenFinal = obtenerNombresConceptos(conceptos,
                AplicaSobre.AUMENTA_MARGEN);
        String nombresAumentaMargenFinalFormateados = formatearNombresConceptos(nombresConceptosAumentaMargenFinal);
        if (!nombresAumentaMargenFinalFormateados.isEmpty()) {
            formulaGeneralBuilder.append(" + ").append(nombresAumentaMargenFinalFormateados);
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
