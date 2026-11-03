package sv.labmedic.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "plantilla")
public class Plantilla extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "tipo_examen_id")
    public TipoExamen tipoExamen;

    public Integer version;

    // {"campos": ["nombre_paciente", "hemoglobina", ...]}
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    public Map<String, Object> definicion;

    // Ruta relativa a labmedic.storage.root
    @Column(name = "archivo_base", nullable = false)
    public String archivoBase;

    public boolean activa = true;

    @Column(name = "hash_sha256", length = 64)
    public String hashSha256;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    public OffsetDateTime fechaCreacion;

    @SuppressWarnings("unchecked")
    public List<String> campos() {
        Object c = definicion == null ? null : definicion.get("campos");
        return c == null ? List.of() : (List<String>) c;
    }
}
