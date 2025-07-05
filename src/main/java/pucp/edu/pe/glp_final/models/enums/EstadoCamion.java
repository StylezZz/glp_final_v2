package pucp.edu.pe.glp_final.models.enums;

public enum EstadoCamion {
    MANTENIMIENTO("Mantenimiento"),
    EN_RUTA("En ruta"),
    DISPONIBLE("Disponible"),
    AVERIADO("Averiado");

    private final String value;

    EstadoCamion(String value){
        this.value = value;
    }

    public String getValue(){
        return value;
    }
}
