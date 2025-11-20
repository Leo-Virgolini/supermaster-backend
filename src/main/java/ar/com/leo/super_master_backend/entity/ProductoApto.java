package ar.com.leo.super_master_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "producto_apto", schema = "supermaster")
public class ProductoApto {

    @EmbeddedId
    private ProductoAptoId id;

    @MapsId("idApto")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_apto", nullable = false)
    private Apto apto;

    @MapsId("idProducto")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    public ProductoApto() {
    }

}