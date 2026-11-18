package com.devSenior.sillon.ia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sillon.ia")
public class IaPropiedades {

	private int tiempoConexionMs = 5000;
	private int tiempoRespuestaMs = 20000;
	private Gemini gemini = new Gemini();
	private Openai openai = new Openai();
	private Respaldo respaldo = new Respaldo();

	public int getTiempoConexionMs() {
		return tiempoConexionMs;
	}

	public void setTiempoConexionMs(int tiempoConexionMs) {
		this.tiempoConexionMs = tiempoConexionMs;
	}

	public int getTiempoRespuestaMs() {
		return tiempoRespuestaMs;
	}

	public void setTiempoRespuestaMs(int tiempoRespuestaMs) {
		this.tiempoRespuestaMs = tiempoRespuestaMs;
	}

	public Gemini getGemini() {
		return gemini;
	}

	public void setGemini(Gemini gemini) {
		this.gemini = gemini;
	}

	public Openai getOpenai() {
		return openai;
	}

	public void setOpenai(Openai openai) {
		this.openai = openai;
	}

	public Respaldo getRespaldo() {
		return respaldo;
	}

	public void setRespaldo(Respaldo respaldo) {
		this.respaldo = respaldo;
	}

	public static class Gemini {
		private String llave = "relleno";

		public String getLlave() {
			return llave;
		}

		public void setLlave(String llave) {
			this.llave = llave;
		}
	}

	public static class Openai {
		private String url;
		private String modelo = "gemini-3.8-flash";

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getModelo() {
			return modelo;
		}

		public void setModelo(String modelo) {
			this.modelo = modelo;
		}
	}

	public static class Respaldo {
		private String url;
		private String llave = "relleno";
		private String modelo = "claude-haiku-4-5-20251001";

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getLlave() {
			return llave;
		}

		public void setLlave(String llave) {
			this.llave = llave;
		}

		public String getModelo() {
			return modelo;
		}

		public void setModelo(String modelo) {
			this.modelo = modelo;
		}
	}

}
