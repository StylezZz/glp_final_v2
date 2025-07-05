package pucp.edu.pe.glp_final.models.enums;

public enum TipoMantenimiento {
    PREVENTIVO("Preventivo"),
    CORRECTIVO("Correctivo");
    private final String value;
    TipoMantenimiento(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
