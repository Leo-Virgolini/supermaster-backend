package ar.com.leo.super_master_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@Entity
@Table(name = "producto_catalogo", schema = "supermaster")
public class ProductoCatalogo {

    @EmbeddedId
    private ProductoCatalogoId id;

    // ---------------------------
    // RELACIÓN PRODUCTO
    // ---------------------------
    @MapsId("idProducto")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    // ---------------------------
    // RELACIÓN CATALOGO
    // ---------------------------
    @MapsId("idCatalogo")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "id_catalogo", nullable = false)
    private Catalogo catalogo;

    public ProductoCatalogo() {
    }

    public ProductoCatalogo(Producto producto, Catalogo catalogo) {
        this.producto = producto;
        this.catalogo = catalogo;
        this.id = new ProductoCatalogoId(producto.getId(), catalogo.getId());
    }

}