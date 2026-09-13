package cl.duoc.digitalfix.bff.contexto;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion de como se resuelve el contexto del usuario.
 *
 * modoDesarrollo existe solo mientras el tenant de identidad no este creado
 * (T-02) y HU-05 no configure el resource server. Cuando eso ocurra, este
 * bloque se borra y el contexto pasa a salir unicamente del JWT.
 */
@ConfigurationProperties(prefix = "digitalfix.contexto")
public class PropiedadesContexto {

    private boolean modoDesarrollo = false;
    private String oidPorDefecto = "dev-oid-0001";
    private Long empresaPorDefecto = 1L;
    private String rolesPorDefecto = "Admin";

    public boolean isModoDesarrollo() {
        return modoDesarrollo;
    }

    public void setModoDesarrollo(boolean modoDesarrollo) {
        this.modoDesarrollo = modoDesarrollo;
    }

    public String getOidPorDefecto() {
        return oidPorDefecto;
    }

    public void setOidPorDefecto(String oidPorDefecto) {
        this.oidPorDefecto = oidPorDefecto;
    }

    public Long getEmpresaPorDefecto() {
        return empresaPorDefecto;
    }

    public void setEmpresaPorDefecto(Long empresaPorDefecto) {
        this.empresaPorDefecto = empresaPorDefecto;
    }

    public String getRolesPorDefecto() {
        return rolesPorDefecto;
    }

    public void setRolesPorDefecto(String rolesPorDefecto) {
        this.rolesPorDefecto = rolesPorDefecto;
    }
}
