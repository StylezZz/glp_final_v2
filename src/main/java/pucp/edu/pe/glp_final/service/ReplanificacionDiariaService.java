package pucp.edu.pe.glp_final.service;

import org.springframework.stereotype.Service;
import pucp.edu.pe.glp_final.algorithm.Mapa;
import pucp.edu.pe.glp_final.algorithm.NodoMapa;
import pucp.edu.pe.glp_final.algorithm.PlanificadorRuta;
import pucp.edu.pe.glp_final.models.Bloqueo;
import pucp.edu.pe.glp_final.models.Camion;
import pucp.edu.pe.glp_final.models.Pedido;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReplanificacionDiariaService {

    /**
     * Replanificación específica para simulación día a día
     * Enfocada en respuesta rápida a pedidos críticos
     */
    public void replanificarDiaria(
            List<Pedido> pedidosNuevosCriticos,
            List<Camion> camiones,
            Mapa mapa,
            List<Bloqueo> bloqueos,
            double timerActual,
            int anio, int mes, int dia, int hora, int minuto,
            LocalDateTime fechaBaseSimulacion
    ) {
        System.out.println("🚨 REPLANIFICACIÓN CRÍTICA DÍA A DÍA - Timer: " + timerActual);

        // 1. Clasificar pedidos por urgencia extrema
        List<Pedido> pedidosUrgentes = clasificarPedidosUrgentes(pedidosNuevosCriticos, timerActual);

        if (pedidosUrgentes.isEmpty()) {
            System.out.println("ℹ️ No hay pedidos urgentes que requieran replanificación");
            return;
        }

        // 2. Evaluar estrategia de reasignación
        EstrategiaReasignacion estrategia = evaluarEstrategia(pedidosUrgentes, camiones, timerActual);

        // 3. Ejecutar estrategia seleccionada
        ejecutarEstrategia(estrategia, camiones, mapa, bloqueos,
                anio, mes, dia, hora, minuto, fechaBaseSimulacion, timerActual);

        System.out.println("✅ Replanificación diaria completada - " +
                estrategia.camionesReasignados.size() + " camiones afectados");
    }

    /**
     * Clasifica pedidos que requieren atención inmediata en simulación diaria
     */
    private List<Pedido> clasificarPedidosUrgentes(List<Pedido> pedidosNuevos, double timerActual) {
        return pedidosNuevos.stream()
                .filter(p -> esUrgenteDiario(p, timerActual))
                .sorted((p1, p2) -> {
                    // Ordenar por tiempo restante (más urgente primero)
                    double tiempo1 = p1.getTiempoLlegada() - timerActual;
                    double tiempo2 = p2.getTiempoLlegada() - timerActual;
                    return Double.compare(tiempo1, tiempo2);
                })
                .collect(Collectors.toList());
    }

    /**
     * Determina si un pedido es urgente en contexto diario
     */
    private boolean esUrgenteDiario(Pedido pedido, double timerActual) {
        double tiempoRestante = pedido.getTiempoLlegada() - timerActual;
        double tiempoViajeDesdeAlmacen = calcularTiempoViajeDesdeAlmacenCentral(pedido);

        // CRITERIO CRÍTICO: Menos de 1.5 veces el tiempo de viaje mínimo
        boolean tiempoCritico = tiempoRestante < (tiempoViajeDesdeAlmacen * 1.5);

        // CRITERIO ADICIONAL: Pedidos con menos de 2 horas restantes
        boolean tiempoAbsoluto = tiempoRestante < 120; // 2 horas

        return tiempoCritico || tiempoAbsoluto;
    }

    /**
     * Evalúa la mejor estrategia de reasignación
     */
    private EstrategiaReasignacion evaluarEstrategia(
            List<Pedido> pedidosUrgentes,
            List<Camion> camiones,
            double timerActual
    ) {
        // Estrategia 1: Usar camiones libres/cercanos al almacén
        EstrategiaReasignacion estrategia1 = evaluarCamionesCercanos(pedidosUrgentes, camiones, timerActual);

        // Estrategia 2: Reasignar camiones con rutas menos críticas
        EstrategiaReasignacion estrategia2 = evaluarReasignacionCompleta(pedidosUrgentes, camiones, timerActual);

        // Seleccionar la estrategia con menor costo
        return (estrategia1.costoTotal < estrategia2.costoTotal) ? estrategia1 : estrategia2;
    }

    /**
     * Estrategia 1: Usar camiones disponibles o cercanos al almacén central
     */
    private EstrategiaReasignacion evaluarCamionesCercanos(
            List<Pedido> pedidosUrgentes,
            List<Camion> camiones,
            double timerActual
    ) {
        EstrategiaReasignacion estrategia = new EstrategiaReasignacion();
        estrategia.tipo = "CAMIONES_CERCANOS";

        List<Camion> camionesDisponibles = camiones.stream()
                .filter(c -> !c.isEnAveria())
                .filter(c -> esCamionDisponibleOCercano(c, timerActual))
                .sorted((c1, c2) -> {
                    double dist1 = distanciaAlAlmacenCentral(c1);
                    double dist2 = distanciaAlAlmacenCentral(c2);
                    return Double.compare(dist1, dist2);
                })
                .collect(Collectors.toList());

        // Asignar pedidos urgentes a camiones disponibles
        int i = 0;
        for (Pedido pedido : pedidosUrgentes) {
            if (i < camionesDisponibles.size()) {
                Camion camion = camionesDisponibles.get(i);
                if (camion.tieneCapacidad(pedido.getCantidadGLP())) {
                    ReasignacionCamion reasignacion = new ReasignacionCamion();
                    reasignacion.camion = camion;
                    reasignacion.pedidosNuevos = Arrays.asList(pedido);
                    reasignacion.costo = calcularCostoReasignacionDiaria(camion, pedido, timerActual);

                    estrategia.camionesReasignados.add(reasignacion);
                    estrategia.costoTotal += reasignacion.costo;
                    i++;
                }
            }
        }

        return estrategia;
    }

    /**
     * Estrategia 2: Reasignación completa basada en criticidad
     */
    private EstrategiaReasignacion evaluarReasignacionCompleta(
            List<Pedido> pedidosUrgentes,
            List<Camion> camiones,
            double timerActual
    ) {
        EstrategiaReasignacion estrategia = new EstrategiaReasignacion();
        estrategia.tipo = "REASIGNACION_COMPLETA";

        // Evaluar todos los camiones para cada pedido urgente
        for (Pedido pedidoUrgente : pedidosUrgentes) {
            Camion mejorCamion = null;
            double menorCosto = Double.MAX_VALUE;

            for (Camion camion : camiones) {
                if (camion.isEnAveria() || !camion.tieneCapacidad(pedidoUrgente.getCantidadGLP())) {
                    continue;
                }

                double costo = calcularCostoReasignacionDiaria(camion, pedidoUrgente, timerActual);
                if (costo < menorCosto) {
                    menorCosto = costo;
                    mejorCamion = camion;
                }
            }

            if (mejorCamion != null && menorCosto < UMBRAL_COSTO_CRITICO) {
                ReasignacionCamion reasignacion = new ReasignacionCamion();
                reasignacion.camion = mejorCamion;
                reasignacion.pedidosNuevos = Arrays.asList(pedidoUrgente);
                reasignacion.costo = menorCosto;

                estrategia.camionesReasignados.add(reasignacion);
                estrategia.costoTotal += menorCosto;
            }
        }

        return estrategia;
    }

    /**
     * Ejecuta la estrategia de reasignación seleccionada
     */
    private void ejecutarEstrategia(
            EstrategiaReasignacion estrategia,
            List<Camion> camiones,
            Mapa mapa,
            List<Bloqueo> bloqueos,
            int anio, int mes, int dia, int hora, int minuto,
            LocalDateTime fechaBaseSimulacion,
            double timerActual
    ) {
        System.out.println("🔄 Ejecutando estrategia: " + estrategia.tipo);

        for (ReasignacionCamion reasignacion : estrategia.camionesReasignados) {
            ejecutarReasignacionDiaria(
                    reasignacion, mapa, bloqueos, anio, mes, dia, hora, minuto,
                    fechaBaseSimulacion, timerActual
            );
        }
    }

    /**
     * Ejecuta la reasignación individual de un camión
     */
    private void ejecutarReasignacionDiaria(
            ReasignacionCamion reasignacion,
            Mapa mapa,
            List<Bloqueo> bloqueos,
            int anio, int mes, int dia, int hora, int minuto,
            LocalDateTime fechaBaseSimulacion,
            double timerActual
    ) {
        Camion camion = reasignacion.camion;

        System.out.println("🚛 Reasignando camión " + camion.getCodigo() +
                " para " + reasignacion.pedidosNuevos.size() + " pedidos urgentes");

        // 1. Cancelar ruta actual (liberar pedidos no entregados)
        cancelarRutaActualDiaria(camion, timerActual);

        // 2. Resetear camión para nueva asignación
        camion.setPedidosAsignados(new ArrayList<>());
        camion.setRoute(new ArrayList<>());

        // 3. Asignar pedidos urgentes
        for (Pedido pedido : reasignacion.pedidosNuevos) {
            camion.getPedidosAsignados().add(pedido);
            camion.asignar(pedido);
            pedido.setEntregado(true);
            pedido.setIdCamion(camion.getCodigo());
        }

        // 4. Generar nueva ruta desde posición actual
        generarRutaUrgenteDiaria(
                camion, mapa, bloqueos, anio, mes, dia, hora, minuto,
                fechaBaseSimulacion, timerActual
        );

        System.out.println("✅ Camión " + camion.getCodigo() + " reasignado exitosamente");
    }

    /**
     * Genera ruta urgente optimizada para simulación diaria
     */
    private void generarRutaUrgenteDiaria(
            Camion camion, Mapa mapa, List<Bloqueo> bloqueos,
            int anio, int mes, int dia, int hora, int minuto,
            LocalDateTime fechaBaseSimulacion, double timerActual
    ) {
        PlanificadorRuta planificador = new PlanificadorRuta();
        NodoMapa posicionActual = camion.getUbicacionActual();

        // Si no tiene posición, empezar desde almacén central
        if (posicionActual == null) {
            posicionActual = new NodoMapa(0, 12, 8, true);
            posicionActual.setAnio(anio);
            posicionActual.setMes(mes);
            posicionActual.setTiempoInicio(timerActual);
            camion.setUbicacionActual(posicionActual);
        }

        // Crear variable final para usar en lambda
        final NodoMapa posicionParaOrdenar = posicionActual;

        // Ordenar pedidos por proximidad para optimizar ruta
        List<Pedido> pedidosOrdenados = camion.getPedidosAsignados().stream()
                .sorted((p1, p2) -> {
                    double dist1 = calcularDistancia(posicionParaOrdenar, p1);
                    double dist2 = calcularDistancia(posicionParaOrdenar, p2);
                    return Double.compare(dist1, dist2);
                })
                .collect(Collectors.toList());

        // Generar ruta para cada pedido
        NodoMapa posicionIterable = posicionActual; // Variable separada para el bucle
        for (Pedido pedido : pedidosOrdenados) {
            NodoMapa nodoPedido = new NodoMapa(0, pedido.getPosX(), pedido.getPosY(), false);

            planificador.encontrarCamino(
                    mapa, posicionIterable, nodoPedido, bloqueos,
                    anio, mes, dia, hora, minuto, camion, pedido,
                    1, // Simulación diaria
                    fechaBaseSimulacion
            );

            posicionIterable = camion.getUbicacionActual();
        }
    }

    // Métodos auxiliares específicos para simulación diaria

    private boolean esCamionDisponibleOCercano(Camion camion, double timerActual) {
        // Camión está libre
        if (camion.getPedidosAsignados() == null || camion.getPedidosAsignados().isEmpty()) {
            return true;
        }

        // Camión está cerca del almacén central
        double distancia = distanciaAlAlmacenCentral(camion);
        return distancia < 10.0; // Radio de cercanía
    }

    private double distanciaAlAlmacenCentral(Camion camion) {
        if (camion.getUbicacionActual() == null) return 0.0;

        return Math.sqrt(
                Math.pow(camion.getUbicacionActual().getX() - 12, 2) +
                        Math.pow(camion.getUbicacionActual().getY() - 8, 2)
        );
    }

    private double calcularTiempoViajeDesdeAlmacenCentral(Pedido pedido) {
        double distancia = Math.sqrt(
                Math.pow(pedido.getPosX() - 12, 2) + Math.pow(pedido.getPosY() - 8, 2)
        );
        return distancia; // 1 minuto por unidad de distancia en simulación diaria
    }

    private double calcularCostoReasignacionDiaria(Camion camion, Pedido pedido, double timerActual) {
        double distancia = calcularDistancia(camion.getUbicacionActual(), pedido);
        double urgencia = calcularUrgencia(pedido, timerActual);

        // En simulación diaria, priorizar urgencia sobre distancia
        return distancia + (urgencia * 50); // Factor de urgencia alto
    }

    private double calcularUrgencia(Pedido pedido, double timerActual) {
        double tiempoRestante = pedido.getTiempoLlegada() - timerActual;
        return Math.max(0, 240 - tiempoRestante) / 240; // Urgencia máxima a 4 horas
    }

    private double calcularDistancia(NodoMapa desde, Pedido pedido) {
        if (desde == null) return Double.MAX_VALUE;
        return Math.sqrt(
                Math.pow(pedido.getPosX() - desde.getX(), 2) +
                        Math.pow(pedido.getPosY() - desde.getY(), 2)
        );
    }

    private void cancelarRutaActualDiaria(Camion camion, double timerActual) {
        if (camion.getPedidosAsignados() != null) {
            for (Pedido pedido : camion.getPedidosAsignados()) {
                if (!pedido.isEntregado()) {
                    pedido.setEntregado(false);
                    pedido.setAsignado(false);
                    pedido.setIdCamion(null);
                    System.out.println("📦 Liberado pedido " + pedido.getId() + " del camión " + camion.getCodigo());
                }
            }
        }

        // Limpiar ruta futura
        if (camion.getRoute() != null) {
            camion.getRoute().removeIf(nodo -> nodo.getTiempoInicio() > timerActual);
        }
    }

    // Clases auxiliares
    private static class EstrategiaReasignacion {
        String tipo;
        List<ReasignacionCamion> camionesReasignados = new ArrayList<>();
        double costoTotal = 0.0;
    }

    private static class ReasignacionCamion {
        Camion camion;
        List<Pedido> pedidosNuevos;
        double costo;
    }

    // Constantes para simulación diaria
    private static final double UMBRAL_COSTO_CRITICO = 200.0; // Más permisivo para casos críticos
}