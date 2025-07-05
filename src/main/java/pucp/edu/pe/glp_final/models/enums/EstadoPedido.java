package pucp.edu.pe.glp_final.models.enums;

public enum EstadoPedido {
    PENDIENTE("Pendiente"),
    ASIGNADO("Asignado"),
    EN_RUTA("En ruta"),
    ENTREGADO("Entregado"),
    NO_ENTREGADO("No entregado");

    private final String value;

    EstadoPedido(String value){
        this.value = value;
    }

    public String getValue(){
        return value;
    }
}
