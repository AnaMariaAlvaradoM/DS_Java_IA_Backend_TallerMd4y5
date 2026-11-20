package com.devSenior.sillon.ia.http;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.devSenior.sillon.ia.config.IaPropiedades;

@Component
public class FabricaClienteHttp {

	private final IaPropiedades propiedades;

	public FabricaClienteHttp(IaPropiedades propiedades) {
		this.propiedades = propiedades;
	}

	public RestClient crear(String urlBase) {
		return builder().baseUrl(urlBase).build();
	}

	public RestClient crear() {
		return builder().build();
	}

	private RestClient.Builder builder() {
		HttpClient http = HttpClient.newBuilder()
				.connectTimeout(Duration.ofMillis(propiedades.getTiempoConexionMs()))
				.build();
		JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
		fabrica.setReadTimeout(Duration.ofMillis(propiedades.getTiempoRespuestaMs()));
		return RestClient.builder().requestFactory(fabrica);
	}

}
