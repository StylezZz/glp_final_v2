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
import pucp.edu.pe.glp_final.algorithm.NodoMapa;

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
    private List<NodoMapa> route;

    @Transient
    public NodoMapa ubicacionActual;

    private boolean capacidadCompleta;
    private double cargaAsignada;
    private double tiempoViaje;
    private Integer tipoAveria;
    private boolean enAveria;
    private Double tiempoInicioAveria;
    private Double tiempoFinAveria;
    private Double glpDisponible;
    private boolean detenido;
    private double tiempoDetenido;
    private double cargaAnterior;

    @Transient
    private List<Pedido> pedidosAsignados;

    public void crearRuta() {
        this.route = new ArrayList<>();
    }

    public void asignar(Pedido pedido) {
        cargaAsignada += pedido.getCantidadGLP();
    }

    public boolean tieneCapacidad(int demanda) {
        return ((cargaAsignada + demanda <= glpDisponible));
    }

    public void posicionar(NodoMapa node) {
        this.ubicacionActual = node;
        route.add(node);
    }

    public void reiniciar() {
        // Reiniciar ruta
        if (this.route != null) this.route.clear();
        else this.crearRuta();

        // Reiniciar valores numéricos
        this.cargaAsignada = 0;
        this.distanciaRecorrida = 0.0;
        this.tiempoViaje = 0;
        this.tipoAveria = 0;
        this.tiempoDetenido = 0;

        // Reiniciar booleanos
        this.capacidadCompleta = false;
        this.enAveria = false;
        this.detenido = false;

        // Reiniciar referencias
        this.ubicacionActual = null;
        this.tiempoInicioAveria = null;
        this.tiempoFinAveria = null;

        // Restablecer valores derivados
        this.glpDisponible = this.carga;
        this.cargaAnterior = 0;

        // Reiniciar pedidos
        if (this.pedidosAsignados != null) this.pedidosAsignados.clear();
        else this.pedidosAsignados = new ArrayList<>();
    }

}
