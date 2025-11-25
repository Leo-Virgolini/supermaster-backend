package ar.com.leo.super_master_backend.dominio.producto.service;

import ar.com.leo.super_master_backend.dominio.apto.entity.Apto;
import ar.com.leo.super_master_backend.dominio.apto.repository.AptoRepository;
import ar.com.leo.super_master_backend.dominio.canal.entity.Canal;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalRepository;
import ar.com.leo.super_master_backend.dominio.catalogo.entity.Catalogo;
import ar.com.leo.super_master_backend.dominio.cliente.entity.Cliente;
import ar.com.leo.super_master_backend.dominio.cliente.repository.ClienteRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoAptoRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalPrecioRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCatalogoRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoClienteRepository;
import ar.com.leo.super_master_backend.dominio.producto.entity.*;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final ProductoCatalogoRepository productoCatalogoRepository;
    private final AptoRepository aptoRepository;
    private final ProductoAptoRepository productoAptoRepository;
    private final CanalRepository canalRepository;
    private final ProductoCanalRepository productoCanalRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoClienteRepository productoClienteRepository;
    private final ProductoCanalPrecioRepository productoCanalPrecioRepository;

    // --------------------
    // CRUD BÁSICO
    // --------------------

    @Override
    public Producto crear(Producto producto) {
        return productoRepository.save(producto);
    }

    @Override
    public Producto actualizar(Integer id, Producto producto) {
        Producto existente = obtenerPorId(id);
        producto.setId(id);
        return productoRepository.save(producto);
    }

    @Override
    public void eliminar(Integer id) {
        productoRepository.deleteById(id);
    }

    @Override
    public Producto obtenerPorId(Integer id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
    }

    @Override
    public Producto obtenerPorSku(String sku) {
        return productoRepository.findBySku(sku)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
    }

    @Override
    public List<Producto> listar() {
        return productoRepository.findAll();
    }

    // --------------------
    // CATÁLOGOS
    // --------------------

    @Override
    public void agregarCatalogo(Integer idProducto, Integer idCatalogo) {
        Producto producto = obtenerPorId(idProducto);

        ProductoCatalogo pc = new ProductoCatalogo();
        pc.setProducto(producto);
        pc.setCatalogo(new Catalogo(idCatalogo));

        productoCatalogoRepository.save(pc);
    }

    @Override
    public void quitarCatalogo(Integer idProducto, Integer idCatalogo) {
        ProductoCatalogoId id = new ProductoCatalogoId(idProducto, idCatalogo);
        productoCatalogoRepository.deleteById(id);
    }

    // --------------------
    // APTOS
    // --------------------

    @Override
    public void agregarApto(Integer idProducto, Integer idApto) {
        Producto producto = obtenerPorId(idProducto);

        ProductoApto pa = new ProductoApto();
        pa.setProducto(producto);
        pa.setApto(new Apto(idApto));

        productoAptoRepository.save(pa);
    }

    @Override
    public void quitarApto(Integer idProducto, Integer idApto) {
        ProductoAptoId id = new ProductoAptoId(idApto, idProducto);
        productoAptoRepository.deleteById(id);
    }

    // --------------------
    // CANALES
    // --------------------

    @Override
    public void agregarCanal(Integer idProducto, Integer idCanal, BigDecimal margenPorcentaje) {
        ProductoCanal pc = new ProductoCanal();
        pc.setProducto(new Producto(idProducto));
        pc.setCanal(new Canal(idCanal));
        pc.setMargenPorcentaje(margenPorcentaje);

        productoCanalRepository.save(pc);
    }

    @Override
    public void actualizarMargenCanal(Integer idProducto, Integer idCanal, BigDecimal margen) {
        List<ProductoCanal> lista = productoCanalRepository.findByProductoId(idProducto)
                .stream()
                .filter(pc -> pc.getCanal().getId().equals(idCanal))
                .toList();

        if (lista.isEmpty())
            throw new RuntimeException("No existe margen");

        ProductoCanal pc = lista.get(0);
        pc.setMargenPorcentaje(margen);
        productoCanalRepository.save(pc);
    }

    @Override
    public void quitarCanal(Integer idProducto, Integer idCanal) {
        List<ProductoCanal> lista = productoCanalRepository.findByProductoId(idProducto)
                .stream()
                .filter(pc -> pc.getCanal().getId().equals(idCanal))
                .toList();

        lista.forEach(pc -> productoCanalRepository.delete(pc));
    }

    // --------------------
    // CLIENTES
    // --------------------

    @Override
    public void agregarCliente(Integer idProducto, Integer idCliente) {
        ProductoCliente pc = new ProductoCliente();
        pc.setProducto(new Producto(idProducto));
        pc.setCliente(new Cliente(idCliente));

        productoClienteRepository.save(pc);
    }

    @Override
    public void quitarCliente(Integer idProducto, Integer idCliente) {
        ProductoClienteId id = new ProductoClienteId(idProducto, idCliente);
        productoClienteRepository.deleteById(id);
    }

    // --------------------
    // COSTO
    // --------------------

    @Override
    public void actualizarCosto(Integer idProducto, BigDecimal nuevoCosto) {
        Producto producto = obtenerPorId(idProducto);
        producto.setCosto(nuevoCosto);
        productoRepository.save(producto);
    }

    // --------------------
    // PRECIOS (placeholder)
    // --------------------

    @Override
    public BigDecimal calcularPrecioCanal(Integer idProducto, Integer idCanal) {
        // Aquí va TODA la lógica del Excel SUPERMASTER
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calcularPrecioMla(Integer idProducto) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calcularPrecioNube(Integer idProducto) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calcularPrecioGastro(Integer idProducto) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calcularPrecioMayorista(Integer idProducto) {
        return BigDecimal.ZERO;
    }

}