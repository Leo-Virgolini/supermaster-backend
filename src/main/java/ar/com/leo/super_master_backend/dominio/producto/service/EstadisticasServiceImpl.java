package ar.com.leo.super_master_backend.dominio.producto.service;

import ar.com.leo.super_master_backend.dominio.producto.dto.EstadisticasDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.EstadisticasDTO.*;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanalPrecio;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalPrecioRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EstadisticasServiceImpl implements EstadisticasService {

    private final ProductoRepository productoRepository;
    private final ProductoCanalPrecioRepository precioRepository;

    @Override
    @Transactional(readOnly = true)
    public EstadisticasDTO obtenerEstadisticas() {
        List<Producto> productos = productoRepository.findAllWithProveedor();
        List<ProductoCanalPrecio> precios = precioRepository.findAllWithCanalAndProducto();

        int totalProductos = productos.size();
        int productosActivos = (int) productos.stream().filter(p -> Boolean.TRUE.equals(p.getActivo())).count();
        int productosSinStock = (int) productos.stream().filter(p -> p.getStock() == null || p.getStock() <= 0).count();
        int productosSinCosto = (int) productos.stream().filter(p -> p.getCosto() == null || p.getCosto().compareTo(BigDecimal.ZERO) <= 0).count();

        Set<Integer> productosConPrecio = precios.stream()
                .map(p -> p.getProducto().getId())
                .collect(Collectors.toSet());
        int productosSinMargen = (int) productos.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .filter(p -> !productosConPrecio.contains(p.getId()))
                .count();

        int productosMargenNegativo = (int) precios.stream()
                .filter(p -> p.getGanancia() != null && p.getGanancia().compareTo(BigDecimal.ZERO) < 0)
                .map(p -> p.getProducto().getId())
                .distinct()
                .count();

        List<ProductosPorCanal> productosPorCanal = calcularProductosPorCanal(precios);
        List<ProductosPorProveedor> productosPorProveedor = calcularProductosPorProveedor(productos);

        List<ProductoMargenNegativo> productosConMargenNegativo = precios.stream()
                .filter(p -> p.getGanancia() != null && p.getGanancia().compareTo(BigDecimal.ZERO) < 0)
                .sorted(Comparator.comparing(ProductoCanalPrecio::getGanancia))
                .limit(50)
                .map(p -> new ProductoMargenNegativo(
                        p.getProducto().getId(),
                        p.getProducto().getSku(),
                        p.getProducto().getDescripcion(),
                        p.getCanal().getCanal(),
                        p.getCuotas(),
                        p.getMargenSobrePvp(),
                        p.getGanancia()
                ))
                .toList();

        List<Integer> cuotasDisponibles = precios.stream()
                .map(p -> p.getCuotas() == null ? 0 : p.getCuotas())
                .distinct()
                .sorted()
                .toList();

        return new EstadisticasDTO(
                totalProductos,
                productosActivos,
                productosSinStock,
                productosSinCosto,
                productosSinMargen,
                productosMargenNegativo,
                productosPorCanal,
                productosPorProveedor,
                productosConMargenNegativo,
                cuotasDisponibles
        );
    }

    @Override
    @Transactional(readOnly = true)
    public MargenesPorCuotasDTO obtenerMargenesPorCuotas(Integer cuotas) {
        List<ProductoCanalPrecio> precios = precioRepository.findAllWithCanalAndProducto();
        List<MargenPorCanal> margenesPorCanal = calcularMargenesPorCanal(precios, cuotas);
        DistribucionMargenes distribucionMargenes = calcularDistribucion(precios, cuotas);
        return new MargenesPorCuotasDTO(margenesPorCanal, distribucionMargenes);
    }

    private List<MargenPorCanal> calcularMargenesPorCanal(List<ProductoCanalPrecio> precios, Integer cuotas) {
        Map<Integer, List<ProductoCanalPrecio>> porCanal = precios.stream()
                .filter(p -> coincideCuotas(p, cuotas))
                .filter(p -> p.getMargenSobrePvp() != null)
                .collect(Collectors.groupingBy(p -> p.getCanal().getId()));

        return porCanal.entrySet().stream()
                .map(entry -> {
                    List<ProductoCanalPrecio> lista = entry.getValue();
                    String nombre = lista.getFirst().getCanal().getCanal();

                    BigDecimal margenProm = promedio(lista.stream().map(ProductoCanalPrecio::getMargenSobrePvp).toList());
                    BigDecimal markupProm = promedio(lista.stream().map(ProductoCanalPrecio::getMarkupPorcentaje).filter(Objects::nonNull).toList());
                    BigDecimal gananciaProm = promedio(lista.stream().map(ProductoCanalPrecio::getGanancia).filter(Objects::nonNull).toList());

                    return new MargenPorCanal(entry.getKey(), nombre, margenProm, markupProm, gananciaProm, lista.size());
                })
                .sorted(Comparator.comparing(MargenPorCanal::canalNombre))
                .toList();
    }

    private List<ProductosPorCanal> calcularProductosPorCanal(List<ProductoCanalPrecio> precios) {
        Map<Integer, List<ProductoCanalPrecio>> porCanal = precios.stream()
                .collect(Collectors.groupingBy(p -> p.getCanal().getId()));

        return porCanal.entrySet().stream()
                .map(entry -> {
                    String nombre = entry.getValue().getFirst().getCanal().getCanal();
                    int cantidadProductos = (int) entry.getValue().stream()
                            .map(p -> p.getProducto().getId())
                            .distinct()
                            .count();
                    return new ProductosPorCanal(entry.getKey(), nombre, cantidadProductos);
                })
                .sorted(Comparator.comparing(ProductosPorCanal::cantidad).reversed())
                .toList();
    }

    private DistribucionMargenes calcularDistribucion(List<ProductoCanalPrecio> precios, Integer cuotas) {
        List<BigDecimal> margenes = precios.stream()
                .filter(p -> coincideCuotas(p, cuotas))
                .map(ProductoCanalPrecio::getMargenSobrePvp)
                .filter(Objects::nonNull)
                .toList();

        int negativo = 0, r0a10 = 0, r10a20 = 0, r20a30 = 0, r30a50 = 0, rMayor50 = 0;
        for (BigDecimal m : margenes) {
            double v = m.doubleValue();
            if (v < 0) negativo++;
            else if (v < 10) r0a10++;
            else if (v < 20) r10a20++;
            else if (v < 30) r20a30++;
            else if (v < 50) r30a50++;
            else rMayor50++;
        }
        return new DistribucionMargenes(negativo, r0a10, r10a20, r20a30, r30a50, rMayor50);
    }

    private List<ProductosPorProveedor> calcularProductosPorProveedor(List<Producto> productos) {
        Map<Integer, List<Producto>> porProveedor = productos.stream()
                .filter(p -> p.getProveedor() != null)
                .collect(Collectors.groupingBy(p -> p.getProveedor().getId()));

        return porProveedor.entrySet().stream()
                .map(entry -> {
                    String nombre = entry.getValue().getFirst().getProveedor().getApodo();
                    return new ProductosPorProveedor(entry.getKey(), nombre, entry.getValue().size());
                })
                .sorted(Comparator.comparing(ProductosPorProveedor::cantidad).reversed())
                .limit(10)
                .toList();
    }

    private static boolean coincideCuotas(ProductoCanalPrecio p, Integer cuotas) {
        int cuotasPrecio = p.getCuotas() == null ? 0 : p.getCuotas();
        int cuotasFiltro = cuotas == null ? 0 : cuotas;
        return cuotasPrecio == cuotasFiltro;
    }

    private BigDecimal promedio(List<BigDecimal> valores) {
        if (valores.isEmpty()) return BigDecimal.ZERO;
        BigDecimal suma = valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return suma.divide(BigDecimal.valueOf(valores.size()), 2, RoundingMode.HALF_UP);
    }
}
