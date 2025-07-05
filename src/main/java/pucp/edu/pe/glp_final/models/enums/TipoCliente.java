package pucp.edu.pe.glp_final.models.enums;

public enum TipoCliente {
    PLANTA_INDUSTRIALES("Plantas industriales"),
    EMPRESAS_COMERCIALES("Empresas comerciales"),
    CONDOMINIOS("Condominios");

    private final String value;

    TipoCliente(String value){
        this.value = value;
    }

    public String getValue(){
        return value;
    }
}
