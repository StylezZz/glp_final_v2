package pucp.edu.pe.glp_final.models.enums;

public enum EstadoMantenimiento {
    PENDIENTE("Pendiente"),
    EN_CURSO("En curso"),
    FINALIZADO("Finalizado");

    public String value;

    EstadoMantenimiento(String value){
        this.value = value;
    }

    public String getValue(){
        return value;
    }
}
