package com.devsenior.sillon.pago;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sillon.stripe")
public class StripeProperties {
    private String urlApi;
    private String claveSecreta;
    private String secretoWebhook;
    private String precioPro;
    private String precioCreditos;

    public String getUrlApi() { return urlApi; }
    public void setUrlApi(String urlApi) { this.urlApi = urlApi; }
    public String getClaveSecreta() { return claveSecreta; }
    public void setClaveSecreta(String claveSecreta) { this.claveSecreta = claveSecreta; }
    public String getSecretoWebhook() { return secretoWebhook; }
    public void setSecretoWebhook(String secretoWebhook) { this.secretoWebhook = secretoWebhook; }
    public String getPrecioPro() { return precioPro; }
    public void setPrecioPro(String precioPro) { this.precioPro = precioPro; }
    public String getPrecioCreditos() { return precioCreditos; }
    public void setPrecioCreditos(String precioCreditos) { this.precioCreditos = precioCreditos; }
}
