package ar.com.leo.super_master_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Embeddable
public class ProductoCatalogoId implements Serializable {

    private static final long serialVersionUID = 8657710790186734706L;

    @NotNull
    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    @NotNull
    @Column(name = "id_catalogo", nullable = false)
    private Integer idCatalogo;

    public ProductoCatalogoId() {
    }

    public ProductoCatalogoId(Integer idProducto, Integer idCatalogo) {
        this.idProducto = idProducto;
        this.idCatalogo = idCatalogo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductoCatalogoId)) return false;
        ProductoCatalogoId that = (ProductoCatalogoId) o;
        return Objects.equals(idProducto, that.idProducto) &&
                Objects.equals(idCatalogo, that.idCatalogo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idProducto, idCatalogo);
    }

}