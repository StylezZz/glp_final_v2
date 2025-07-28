package pucp.edu.pe.glp_final.models.enums;

public enum TipoIncidente {
    LEVE("Leve"),
    MODERADO("Moderado"),
    GRAVE("Grave"),
    MANTENIMIENTO("Mantenimiento");

    public String value;

    TipoIncidente(String value) {
        this.value = value;
    }

}
