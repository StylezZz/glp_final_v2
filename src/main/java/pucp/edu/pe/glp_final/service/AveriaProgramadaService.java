package pucp.edu.pe.glp_final.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pucp.edu.pe.glp_final.algorithm.NodoMapa;
import pucp.edu.pe.glp_final.models.*;
import pucp.edu.pe.glp_final.models.enums.TipoIncidente;
import pucp.edu.pe.glp_final.repository.AveriaProgramadaRepository;
import pucp.edu.pe.glp_final.repository.AveriaGeneradaRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AveriaProgramadaService {

    @Autowired
    private AveriaProgramadaRepository averiaProgramadaRepository;

    @Autowired
    private AveriaGeneradaRepository averiaGeneradaRepository;

    /**
     * Genera averías probabilísticamente basadas en averías programadas
     */
    public List<Averia> generarAveriasProbabilisticas(
            Long simulacionId,
            LocalDateTime fechaBaseSimulacion,
            int turnoActual,
            List<Camion> camionesEnOperacion,
            double momento
    ) {
        List<AveriaProgramada> averiasProgramadas = averiaProgramadaRepository.findByTurnoAveria(turnoActual);
        System.out.println("Generando averías programadas para el turno " + turnoActual + ": " + averiasProgramadas.size());
        List<Averia> averiasGeneradas = new ArrayList<>();

        for (AveriaProgramada averiaProgramada : averiasProgramadas) {
            // Verificar si el camión está en operación
            Camion camionEnOperacion = camionesEnOperacion.stream()
                    .filter(c -> c.getCodigo().equals(averiaProgramada.getCodigoCamion()))
                    .filter(c -> !c.isEnAveria()) // No está en avería
                    .filter(c -> c.getRoute() != null && !c.getRoute().isEmpty()) // Tiene ruta
                    .filter(c -> estaEjecutandoRuta(c, momento))
                    .findFirst()
                    .orElse(null);

            if (camionEnOperacion != null) {
                // LÓGICA DE PROBABILIDAD: 5% a 35% del recorrido
                System.out.println("Camión en operación: " + camionEnOperacion.getCodigo() +
                        ", Tipo de avería: " + averiaProgramada.getTipoAveria());
                double porcentajeRecorrido = Math.random() * 0.3 + 0.05; // 5% a 35%

                // Verificar si el camión está en ese rango del recorrido
                if (estaEnRangoDeAveria(camionEnOperacion, porcentajeRecorrido, momento)) {
                    // Generar avería
                    System.out.println("Generando avería para el camión: " + camionEnOperacion.getCodigo() +
                            ", Tipo de avería: " + averiaProgramada.getTipoAveria() +
                            ", Porcentaje recorrido: " + (porcentajeRecorrido * 100) + "%");
                    Averia averia = new Averia();
                    averia.setCodigoCamion(averiaProgramada.getCodigoCamion());
                    averia.setTipoAveria(averiaProgramada.getTipoAveria());
                    averia.setTurnoAveria(turnoActual);
                    averia.setDescripcion("Avería programada - " + averiaProgramada.getTipoAveria() +
                            " en " + porcentajeRecorrido * 100 + "% del recorrido");

                    averiasGeneradas.add(averia);

                    // Guardar en base de datos
                    guardarAveriaGenerada(simulacionId, fechaBaseSimulacion, averia, momento, porcentajeRecorrido);
                }
            }
        }

        return averiasGeneradas;
    }

    private boolean estaEnAlmacen(Camion camion) {
        if (camion.getUbicacionActual() == null) {
            return true; // Si no tiene ubicación, asumimos que está en almacén
        }

        int x = camion.getUbicacionActual().getX();
        int y = camion.getUbicacionActual().getY();

        // ✅ Posiciones de almacenes (deben coincidir con las del frontend)
        boolean estaEnAlmacenCentral = (x == 12 && y == 8);
        boolean estaEnAlmacenEste = (x == 63 && y == 3);
        boolean estaEnAlmacenNorte = (x == 42 && y == 42);

        return estaEnAlmacenCentral || estaEnAlmacenEste || estaEnAlmacenNorte;
    }

    /**
     * Verifica si el camión está en el rango de avería (5%-35% del recorrido)
     */
    private boolean estaEnRangoDeAveria(Camion camion, double porcentajeRecorrido, double timerActual) {
        if (camion.getRoute() == null || camion.getRoute().isEmpty()) {
            return false;
        }

        // ✅ NUEVA LÓGICA: Calcular progreso basado en tiempo transcurrido
        double tiempoInicioRuta = camion.getRoute().get(0).getTiempoInicio();
        double tiempoFinRuta = camion.getRoute().get(camion.getRoute().size() - 1).getTiempoFin();
        System.out.println("Tiempo inicio ruta: " + tiempoInicioRuta +
                ", Tiempo fin ruta: " + tiempoFinRuta + ", Timer actual: " + timerActual);

        // Si no hay diferencia de tiempo, la ruta acaba de empezar
        if (tiempoFinRuta <= tiempoInicioRuta) {
            return false;
        }

        // ✅ Calcular progreso real basado en timer
        double progresoTemporal = (timerActual - tiempoInicioRuta) / (tiempoFinRuta - tiempoInicioRuta);
        System.out.println("Progreso temporal: " + progresoTemporal);

        // ✅ Asegurar que esté en rango válido
        progresoTemporal = Math.max(0.0, Math.min(1.0, progresoTemporal));

        // ✅ Verificar si está en el rango de avería (5%-35%)
        return progresoTemporal >= 0.05 && progresoTemporal <= 0.35;
    }

    /**
     * Guarda la avería generada en la base de datos
     */
    private void guardarAveriaGenerada(Long simulacionId, LocalDateTime fechaBase, Averia averia, double momento, double porcentaje) {
        AveriaGenerada averiaGenerada = new AveriaGenerada();
        averiaGenerada.setSimulacionId(simulacionId);
        averiaGenerada.setFechaBaseSimulacion(fechaBase);
        averiaGenerada.setCodigoCamion(averia.getCodigoCamion());
        averiaGenerada.setTipoAveria(averia.getTipoAveria());
        averiaGenerada.setTurnoAveria(averia.getTurnoAveria());
        averiaGenerada.setMomentoGeneracion(momento);
        averiaGenerada.setPorcentajeRecorrido(porcentaje); // > 0.0 indica que es automática
        averiaGenerada.setDescripcion("Avería automática - " + averia.getDescripcion());
        averiaGenerada.setCreatedAt(LocalDateTime.now());

        averiaGeneradaRepository.save(averiaGenerada);
    }

    /**
     * Guarda una avería manual generada desde la plataforma
     */
    public void guardarAveriaManual(
            Long simulacionId,
            LocalDateTime fechaBaseSimulacion,
            String codigoCamion,
            TipoIncidente tipoAveria,
            int turnoAveria,
            String descripcion,
            double momentoGeneracion
    ) {
        AveriaGenerada averiaGenerada = new AveriaGenerada();
        averiaGenerada.setSimulacionId(simulacionId);
        averiaGenerada.setFechaBaseSimulacion(fechaBaseSimulacion);
        averiaGenerada.setCodigoCamion(codigoCamion);
        averiaGenerada.setTipoAveria(tipoAveria);
        averiaGenerada.setTurnoAveria(turnoAveria);
        averiaGenerada.setMomentoGeneracion(momentoGeneracion);
        averiaGenerada.setPorcentajeRecorrido(0.0); // 0.0 indica que es manual
        averiaGenerada.setDescripcion(descripcion);
        averiaGenerada.setCreatedAt(LocalDateTime.now());

        averiaGeneradaRepository.save(averiaGenerada);
    }

    /**
     * Obtiene averías generadas por simulación
     */
    public List<AveriaGenerada> obtenerAveriasPorSimulacion(Long simulacionId) {
        return averiaGeneradaRepository.findBySimulacionIdOrderByMomentoGeneracionDesc(simulacionId);
    }

    private boolean estaEjecutandoRuta(Camion camion, double timerActual) {
        if (camion.getRoute() == null || camion.getRoute().isEmpty()) {
            return false;
        }

        // ✅ VERIFICACIÓN 1: Temporal - debe estar dentro del rango de tiempo de la ruta
        double tiempoInicioRuta = camion.getRoute().get(0).getTiempoInicio();
        double tiempoFinRuta = camion.getRoute().get(camion.getRoute().size() - 1).getTiempoFin();

        boolean dentroDelRangoTemporal = timerActual >= tiempoInicioRuta && timerActual <= tiempoFinRuta;

        if (!dentroDelRangoTemporal) {
            return false; // Fuera del rango temporal de la ruta
        }

        // ✅ VERIFICACIÓN 2: Progreso - debe tener al menos un nodo en progreso
        for (NodoMapa nodo : camion.getRoute()) {
            boolean nodoEmpezado = timerActual >= nodo.getTiempoInicio();
            boolean nodoNoTerminado = timerActual < nodo.getTiempoFin();

            if (nodoEmpezado && nodoNoTerminado) {
                System.out.println("✅ Camión " + camion.getCodigo() + " ejecutando nodo en tiempo " +
                        timerActual + " (nodo: " + nodo.getTiempoInicio() + "-" + nodo.getTiempoFin() + ")");
                return true; // Hay un nodo actualmente en ejecución
            }
        }

        // ✅ VERIFICACIÓN 3: Si no hay nodo en progreso, verificar que ya empezó pero no terminó
        boolean yaEmpezó = timerActual > tiempoInicioRuta + 2; // Al menos 2 minutos después de empezar
        boolean noTerminó = timerActual < tiempoFinRuta - 2; // Al menos 2 minutos antes de terminar

        return yaEmpezó && noTerminó;
    }

}