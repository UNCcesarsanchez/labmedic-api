package sv.labmedic.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Optional;

@Entity
@Table(name = "tipo_examen")
public class TipoExamen extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true)
    public String codigo;

    @Column(nullable = false)
    public String nombre;

    public String categoria;

    public boolean activo = true;

    // Lo llena la BD (DEFAULT now())
    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    public OffsetDateTime fechaCreacion;

    public static Optional<TipoExamen> porCodigo(String codigo) {
        return find("codigo", codigo).firstResultOptional();
    }
}
