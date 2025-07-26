package pucp.edu.pe.glp_final.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonManagedReference;

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

    public static Bloqueo leerRegistro(String registro, int anio, int mes) {
        Bloqueo bloqueo = new Bloqueo();
        bloqueo.setAnio(anio);
        bloqueo.setMes(mes);

        String[] divsionesTiempo = registro.split(":");
        if (divsionesTiempo.length != 2)
            throw new IllegalArgumentException("Registro mal formado");

        String[] limitesTiempo = divsionesTiempo[0].split("-");
        if (limitesTiempo.length != 2)
            throw new IllegalArgumentException("Fechas límite incorrectas");

        String[] inicio = limitesTiempo[0].split("[dhm]");
        if (inicio.length != 3)
            throw new IllegalArgumentException("Fecha inicio mal formada");

        bloqueo.setDiaInicio(Integer.parseInt(inicio[0].replace("d", "")));
        bloqueo.setHoraInicio(Integer.parseInt(inicio[1].replace("h", "")));
        bloqueo.setMinutoInicio(Integer.parseInt(inicio[2].replace("m", "")));

        String[] fin = limitesTiempo[1].split("[dhm]");
        if (fin.length != 3)
            throw new IllegalArgumentException("Fecha fin mal formada");

        bloqueo.setDiaFin(Integer.parseInt(fin[0].replace("d", "")));
        bloqueo.setHoraFin(Integer.parseInt(fin[1].replace("h", "")));
        bloqueo.setMinutoFin(Integer.parseInt(fin[2].replace("m", "")));

        LocalDateTime fechaInicio = LocalDateTime.of(anio, mes, bloqueo.getDiaInicio(),
                bloqueo.getHoraInicio(), bloqueo.getMinutoInicio());
        LocalDateTime fechaFin = LocalDateTime.of(anio, mes, bloqueo.getDiaFin(),
                bloqueo.getHoraFin(), bloqueo.getMinutoFin());
        bloqueo.setFechaInicio(fechaInicio);
        bloqueo.setFechaFin(fechaFin);

        String[] coordenadas = divsionesTiempo[1].split(",");
        if (coordenadas.length % 2 != 0)
            throw new IllegalArgumentException("Coordenadas mal formadas");

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

    public static List<Bloqueo> obtenerBloqueosActivos(
            List<Bloqueo> bloqueos,
            Calendar momento
    ) {
        List<Bloqueo> bloqueosActivos = new ArrayList<>();
        for (Bloqueo bloqueo : bloqueos)
            if (bloqueo.activo(momento))
                bloqueosActivos.add(bloqueo);
        return bloqueosActivos;
    }

    public boolean activo(Calendar momento) {
        LocalDateTime momentoDateTime = LocalDateTime.of(
                momento.get(Calendar.YEAR),
                momento.get(Calendar.MONTH) + 1,
                momento.get(Calendar.DAY_OF_MONTH),
                momento.get(Calendar.HOUR_OF_DAY),
                momento.get(Calendar.MINUTE)
        );

        return !momentoDateTime.isBefore(fechaInicio) &&
                momentoDateTime.isBefore(fechaFin);
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
