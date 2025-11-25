package ar.com.leo.super_master_backend.dominio.canal.entity;

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
public class CanalConceptoId implements Serializable {

    private static final long serialVersionUID = 6653632297970539527L;

    @NotNull
    @Column(name = "id_canal", nullable = false)
    private Integer idCanal;

    @NotNull
    @Column(name = "id_concepto", nullable = false)
    private Integer idConcepto;

    public CanalConceptoId(Integer idCanal, Integer idConcepto) {
        this.idCanal = idCanal;
        this.idConcepto = idConcepto;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CanalConceptoId)) return false;
        CanalConceptoId that = (CanalConceptoId) o;
        return Objects.equals(idCanal, that.idCanal) &&
                Objects.equals(idConcepto, that.idConcepto);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idCanal, idConcepto);
    }

}