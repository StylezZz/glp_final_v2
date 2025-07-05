package pucp.edu.pe.glp_final.models;

public class MetricasSimulacion {
    private int pedidosEntregados;
    private double consumoTotalCombustible;
    private int rutasGeneradas;
    private double tiempoPromedioEntrega;

    public int getPedidosEntregados() {
        return pedidosEntregados;
    }
    public void setPedidosEntregados(int pedidosEntregados) {
        this.pedidosEntregados = pedidosEntregados;
    }
    public double getConsumoTotalCombustible() {
        return consumoTotalCombustible;
    }
    public void setConsumoTotalCombustible(double consumoTotalCombustible) {
        this.consumoTotalCombustible = consumoTotalCombustible;
    }
    public int getRutasGeneradas() {
        return rutasGeneradas;
    }
    public void setRutasGeneradas(int rutasGeneradas) {
        this.rutasGeneradas = rutasGeneradas;
    }
    public double getTiempoPromedioEntrega() {
        return tiempoPromedioEntrega;
    }
    public void setTiempoPromedioEntrega(double tiempoPromedioEntrega) {
        this.tiempoPromedioEntrega = tiempoPromedioEntrega;
    }
}