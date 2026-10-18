package cl.duoc.digitalfix.bff.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "digitalfix.microservicios")
public class PropiedadesMicroservicios {

    private Destino catalogo = new Destino();
    private Destino usuarios = new Destino();
    private Destino reportes = new Destino();
    private Destino auditoria = new Destino();
    private Destino workorders = new Destino();

    public Destino getCatalogo() { return catalogo; }
    public void setCatalogo(Destino catalogo) { this.catalogo = catalogo; }

    public Destino getUsuarios() { return usuarios; }
    public void setUsuarios(Destino usuarios) { this.usuarios = usuarios; }

    public Destino getReportes() { return reportes; }
    public void setReportes(Destino reportes) { this.reportes = reportes; }

    public Destino getAuditoria() { return auditoria; }
    public void setAuditoria(Destino auditoria) { this.auditoria = auditoria; }

    public Destino getWorkorders() { return workorders; }
    public void setWorkorders(Destino workorders) { this.workorders = workorders; }

    public static class Destino {

        private String url = "http://localhost:8080";
        private int timeoutConexionMs = 2000;
        private int timeoutLecturaMs = 5000;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public int getTimeoutConexionMs() { return timeoutConexionMs; }
        public void setTimeoutConexionMs(int timeoutConexionMs) { this.timeoutConexionMs = timeoutConexionMs; }
        public int getTimeoutLecturaMs() { return timeoutLecturaMs; }
        public void setTimeoutLecturaMs(int timeoutLecturaMs) { this.timeoutLecturaMs = timeoutLecturaMs; }
    }
}