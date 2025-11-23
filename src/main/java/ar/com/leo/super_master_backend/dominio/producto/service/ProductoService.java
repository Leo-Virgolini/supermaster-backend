package ar.com.leo.super_master_backend.dominio.producto.service;

import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;

import java.math.BigDecimal;
import java.util.List;

public interface ProductoService {

    // CRUD básico
    Producto crear(Producto producto);

    Producto actualizar(Integer id, Producto producto);

    void eliminar(Integer id);

    Producto obtenerPorId(Integer id);

    Producto obtenerPorSku(String sku);

    List<Producto> listar();

    // --- RELACIONES ---

    // Catálogos
    void agregarCatalogo(Integer idProducto, Integer idCatalogo);

    void quitarCatalogo(Integer idProducto, Integer idCatalogo);

    // Canales
    void agregarCanal(Integer idProducto, Integer idCanal, BigDecimal margenPorcentaje);

    void actualizarMargenCanal(Integer idProducto, Integer idCanal, BigDecimal margenPorcentaje);

    void quitarCanal(Integer idProducto, Integer idCanal);

    // Aptos
    void agregarApto(Integer idProducto, Integer idApto);

    void quitarApto(Integer idProducto, Integer idApto);

    // Clientes
    void agregarCliente(Integer idProducto, Integer idCliente);

    void quitarCliente(Integer idProducto, Integer idCliente);

    // --- PRECIOS ---

    BigDecimal calcularPrecioCanal(Integer idProducto, Integer idCanal);

    BigDecimal calcularPrecioMla(Integer idProducto);   // precio marketplace

    BigDecimal calcularPrecioNube(Integer idProducto);  // precio web

    BigDecimal calcularPrecioGastro(Integer idProducto);

    BigDecimal calcularPrecioMayorista(Integer idProducto);

    // --- COSTO ---

    void actualizarCosto(Integer idProducto, BigDecimal nuevoCosto);

}
