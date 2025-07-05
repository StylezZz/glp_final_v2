package pucp.edu.pe.glp_final.models.enums;

public enum EstadoRuta {
    PLANIFICADA("Planificada"),
    EN_EJECUCION("En ejecución"),
    COMPLETADA("Completada");

    private final String value;

    EstadoRuta(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

