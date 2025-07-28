package pucp.edu.pe.glp_final.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pucp.edu.pe.glp_final.models.enums.TipoIncidente;

@Entity
@Table(name = "averia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AveriaProgramada {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "turno_averia")
    private Integer turnoAveria;

    @Column(name = "codigo_camion")
    private String codigoCamion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_averia")
    private TipoIncidente tipoAveria;

    private String descripcion;
}