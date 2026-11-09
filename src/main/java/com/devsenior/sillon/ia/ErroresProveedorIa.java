package com.devsenior.sillon.ia;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

final class ErroresProveedorIa {
    private ErroresProveedorIa() { }

    static FalloProveedorIa clasificar(RestClientException error) {
        if (error instanceof RestClientResponseException respuesta) {
            int estado = respuesta.getStatusCode().value();
            return new FalloProveedorIa(estado == 401 || estado == 403
                    ? FalloProveedorIa.Tipo.AUTENTICACION : FalloProveedorIa.Tipo.TEMPORAL, estado);
        }
        Throwable causa = error;
        while (causa != null) {
            if (causa instanceof SocketTimeoutException || causa instanceof HttpTimeoutException) {
                return new FalloProveedorIa(FalloProveedorIa.Tipo.TIMEOUT);
            }
            causa = causa.getCause();
        }
        return new FalloProveedorIa(FalloProveedorIa.Tipo.CONECTIVIDAD);
    }
}
