package pucp.edu.pe.glp_final.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pucp.edu.pe.glp_final.models.enums.TipoIncidente;
import java.time.LocalDateTime;

@Entity
@Table(name = "averia_generada")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AveriaGenerada {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_base_simulacion")
    private LocalDateTime fechaBaseSimulacion;

    @Column(name = "simulacion_id")
    private Long simulacionId;

    @Column(name = "codigo_camion")
    private String codigoCamion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_averia")
    private TipoIncidente tipoAveria;

    @Column(name = "turno_averia")
    private Integer turnoAveria;

    @Column(name = "momento_generacion")
    private Double momentoGeneracion;

    @Column(name = "porcentaje_recorrido")
    private Double porcentajeRecorrido;

    private String descripcion;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}