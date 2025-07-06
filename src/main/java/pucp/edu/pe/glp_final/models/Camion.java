package pucp.edu.pe.glp_final.models;

import java.util.ArrayList;
import java.util.List;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pucp.edu.pe.glp_final.algorithm.NodePosition;

@Table(name = "camion")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity

public class Camion {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private double tara;

    @Column(nullable = false)
    private double carga;

    @Column(name = "peso_carga", nullable = false)
    private double pesoCarga;

    @Column(nullable = false)
    private double peso;

    @Column(nullable = false)
    private double combustible;

    @Column(nullable = false)
    private double distanciaMaxima;

    private Double distanciaRecorrida;

    @Column(nullable = false)
    private double velocidad;

    @Transient
    private List<NodePosition> route;

    private boolean capacidadCompleta;

    private double cargaAsignada;

    private double tiempoViaje;

    @Transient
    public NodePosition ubicacionActual; // ubicación actual del vehículo

    private Integer tipoAveria; // Tipo Averia 0 -> Ninguna Averia, 1 -> Averia Leve, 2 -> Averia Grave, 3 -> Averia
    // Critica
    private boolean enAveria;
    private Double tiempoInicioAveria;
    private Double tiempoFinAveria;
    private Double glpDisponible;
    private boolean detenido;
    private double tiempoDetenido;
    private double cargaAnterior;

    @Transient
    private List<Pedido> pedidosAsignados;

    public Camion(String tipo, int numero, double tara, double carga, double pesoCarga,
                  double peso, double combustible, double velocidad) {
        this.codigo = generarCodigo(tipo, numero);
        this.tara = tara;
        this.carga = carga;
        this.pesoCarga = pesoCarga;
        this.peso = peso;
        this.combustible = combustible;
        this.distanciaMaxima = calcularDistancia(); // Calcular la distancia al crear un camion
        this.velocidad = velocidad;
        this.route = new ArrayList<>();
        this.capacidadCompleta = false;
        this.cargaAsignada = 0;
        this.tiempoViaje = 0;
        this.ubicacionActual = null;
        this.distanciaRecorrida = 0.0;
        this.tipoAveria = 0;
        this.enAveria = false;
        this.glpDisponible = carga;
        this.pedidosAsignados = new ArrayList<>();
        this.cargaAnterior = 0;
    }

    public double calcularDistancia() {
        double distanciaKm = carga * 180 / peso;
        return distanciaKm;
    }

    private String generarCodigo(String tipo, int numero) {
        String formatoTipo = tipo.toUpperCase(); // Asegura que el tipo esté en mayúsculas
        String formatoCorrelativo = String.format("%02d", numero); // Asegura que el correlativo tenga dos dígitos
        return formatoTipo + formatoCorrelativo;
    }

    public void inicializarRuta() {
        this.route = new ArrayList<>();
    }

    public void asignarPedido(Pedido pedido) {
        cargaAsignada += pedido.getCantidadGLP();
    }

    public boolean CheckIfFits(int demanda) {
        return ((cargaAsignada + demanda <= glpDisponible)); // true -> si la demanda adicional puede ser acomodada sin exceder
        // la capacidad
    }

    public void asignarPosicion(NodePosition node) {
        this.ubicacionActual = node;
        route.add(node);
    }

}
