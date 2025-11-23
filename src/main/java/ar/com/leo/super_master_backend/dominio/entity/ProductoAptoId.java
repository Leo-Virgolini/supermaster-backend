package ar.com.leo.super_master_backend.dominio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@Embeddable
public class ProductoAptoId implements Serializable {

    private static final long serialVersionUID = -2162252342941568559L;

    @NotNull
    @Column(name = "id_apto", nullable = false)
    private Integer idApto;

    @NotNull
    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    public ProductoAptoId(Integer idApto, Integer idProducto) {
        this.idApto = idApto;
        this.idProducto = idProducto;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductoAptoId)) return false;
        ProductoAptoId that = (ProductoAptoId) o;
        return Objects.equals(idApto, that.idApto) &&
                Objects.equals(idProducto, that.idProducto);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idApto, idProducto);
    }

}