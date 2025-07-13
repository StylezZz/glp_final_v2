package pucp.edu.pe.glp_final.models;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "bloqueo")
public class Bloqueo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int anio;
    private int mes;
    private int diaInicio;
    private int horaInicio;
    private int minutoInicio;
    private int diaFin;
    private int horaFin;
    private int minutoFin;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;

    @OneToMany(mappedBy = "bloqueo", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<TramoBloqueado> tramo = new ArrayList<>();

    public void addTramo(TramoBloqueado nodoBloqueo) {
        tramo.add(nodoBloqueo);
        nodoBloqueo.setBloqueo(this);
    }

    public static Bloqueo leerBloqueo(String registro, int anio, int mes) {
        Bloqueo bloqueo = new Bloqueo();
        bloqueo.anio = anio;
        bloqueo.mes = mes;

        String[] partes = registro.split(":");
        if (partes.length != 2) {
            throw new IllegalArgumentException("Formato de entrada incorrecto");
        }

        String[] momentoInicioFin = partes[0].split("-");
        if (momentoInicioFin.length != 2) {
            throw new IllegalArgumentException("Formato de inicio/fin incorrecto");
        }

        String[] partesInicio = momentoInicioFin[0].split("[dhm]");
        if (partesInicio.length != 3) {
            throw new IllegalArgumentException("Formato de momento incorrecto");
        }

        bloqueo.setDiaInicio(Integer.parseInt(partesInicio[0].replace("d", "")));
        bloqueo.setHoraInicio(Integer.parseInt(partesInicio[1].replace("h", "")));
        bloqueo.setMinutoInicio(Integer.parseInt(partesInicio[2].replace("m", "")));

        String[] partesFin = momentoInicioFin[1].split("[dhm]");
        if (partesFin.length != 3) {
            throw new IllegalArgumentException("Formato de momento incorrecto");
        }

        bloqueo.setDiaFin(Integer.parseInt(partesFin[0].replace("d", "")));
        bloqueo.setHoraFin(Integer.parseInt(partesFin[1].replace("h", "")));
        bloqueo.setMinutoFin(Integer.parseInt(partesFin[2].replace("m", "")));

        LocalDateTime fechaInicio = LocalDateTime.of(anio, mes, bloqueo.getDiaInicio(),
                bloqueo.getHoraInicio(), bloqueo.getMinutoInicio());
        LocalDateTime fechaFin = LocalDateTime.of(anio, mes, bloqueo.getDiaFin(),
                bloqueo.getHoraFin(), bloqueo.getMinutoFin());
        bloqueo.setFechaInicio(fechaInicio);
        bloqueo.setFechaFin(fechaFin);

        String[] coordenadas = partes[1].split(",");
        if (coordenadas.length % 2 != 0) {
            throw new IllegalArgumentException("Formato de coordenadas incorrecto");
        }

        for (int i = 0; i < coordenadas.length - 2; i += 2) {
            TramoBloqueado tramo = TramoBloqueado.builder()
                    .x_ini(Integer.parseInt(coordenadas[i]))
                    .y_ini(Integer.parseInt(coordenadas[i + 1]))
                    .x_fin(Integer.parseInt(coordenadas[i + 2]))
                    .y_fin(Integer.parseInt(coordenadas[i + 3]))
                    .build();
            bloqueo.addTramo(tramo);
        }

        return bloqueo;
    }

    public boolean estaActivo(Calendar ahora) {
        int anioActual = ahora.get(Calendar.YEAR);
        int mesActual = ahora.get(Calendar.MONTH) + 1;
        int diaActual = ahora.get(Calendar.DAY_OF_MONTH);
        int horaActual = ahora.get(Calendar.HOUR_OF_DAY);
        int minutoActual = ahora.get(Calendar.MINUTE);
        if (
                (anioActual > anio || (anioActual == anio && mesActual > mes))
                        || (anioActual < anio || mesActual < mes || diaActual < diaInicio)
                        || (diaActual == diaInicio && horaActual < horaInicio)
                        || (diaActual == diaInicio && horaActual == horaInicio && minutoActual < minutoInicio)
                        || (diaActual == diaInicio && horaActual == horaInicio &&
                        (diaActual > diaFin || horaActual > horaFin || (horaActual == horaFin && minutoActual >= minutoFin))
                )
        )
            return false;

        return true;
    }

    public static List<Bloqueo> bloqueosActivos(List<Bloqueo> bloqueos, Calendar ahora) {
        List<Bloqueo> bloqueosActivos = new ArrayList<>();
        for (Bloqueo bloqueo : bloqueos)
            if (bloqueo.estaActivo(ahora))
                bloqueosActivos.add(bloqueo);
        return bloqueosActivos;
    }

    public static List<Nodo> NodosBloqueados(List<Bloqueo> bloqueos) {
        List<Nodo> nodosBloqueados = new ArrayList<>();
        for (Bloqueo bloqueo : bloqueos) {
            for (TramoBloqueado tramoBloqueo : bloqueo.getTramo()) {
                if (tramoBloqueo.getY_ini() == tramoBloqueo.getY_fin()) {
                    if (tramoBloqueo.getX_ini() < tramoBloqueo.getX_fin()) {
                        for (int i = tramoBloqueo.getX_ini(); i <= tramoBloqueo.getX_fin(); i++) {
                            Nodo nodo = new Nodo(i, tramoBloqueo.getY_ini());
                            nodosBloqueados.add(nodo);
                        }
                    } else {
                        for (int i = tramoBloqueo.getX_fin(); i <= tramoBloqueo.getX_ini(); i++) {
                            Nodo nodo = new Nodo(i, tramoBloqueo.getY_ini());
                            nodosBloqueados.add(nodo);
                        }
                    }
                } else {
                    if (tramoBloqueo.getY_ini() < tramoBloqueo.getY_fin()) {
                        for (int i = tramoBloqueo.getY_ini(); i <= tramoBloqueo.getY_fin(); i++) {
                            Nodo nodo = new Nodo(tramoBloqueo.getX_ini(), i);
                            nodosBloqueados.add(nodo);
                        }
                    } else {
                        for (int i = tramoBloqueo.getY_fin(); i <= tramoBloqueo.getY_ini(); i++) {
                            Nodo nodo = new Nodo(tramoBloqueo.getX_ini(), i);
                            nodosBloqueados.add(nodo);
                        }
                    }
                }

            }

        }

        return nodosBloqueados;
    }
}
