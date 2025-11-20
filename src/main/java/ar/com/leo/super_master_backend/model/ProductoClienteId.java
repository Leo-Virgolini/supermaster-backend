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
public class ProductoClienteId implements Serializable {

    private static final long serialVersionUID = -5686937608961789449L;

    @NotNull
    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    @NotNull
    @Column(name = "id_cliente", nullable = false)
    private Integer idCliente;

    public ProductoClienteId() {
    }

    public ProductoClienteId(Integer idProducto, Integer idCliente) {
        this.idProducto = idProducto;
        this.idCliente = idCliente;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductoClienteId)) return false;
        ProductoClienteId that = (ProductoClienteId) o;
        return Objects.equals(idProducto, that.idProducto) &&
                Objects.equals(idCliente, that.idCliente);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idProducto, idCliente);
    }

}