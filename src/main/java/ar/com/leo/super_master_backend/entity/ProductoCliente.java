package ar.com.leo.super_master_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "producto_cliente", schema = "supermaster")
public class ProductoCliente {

    @EmbeddedId
    private ProductoClienteId id;

    // ---------------------------
    // RELACIÓN CON PRODUCTO
    // ---------------------------
    @MapsId("idProducto")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    // ---------------------------
    // RELACIÓN CON CLIENTE
    // ---------------------------
    @MapsId("idCliente")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    public ProductoCliente() {
    }

    public ProductoCliente(Producto producto, Cliente cliente) {
        this.producto = producto;
        this.cliente = cliente;
        this.id = new ProductoClienteId(producto.getId(), cliente.getId());
    }

}