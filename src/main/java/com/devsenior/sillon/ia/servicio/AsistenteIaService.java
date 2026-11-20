package com.devsenior.sillon.ia.servicio;

import com.devsenior.sillon.compartido.excepcion.ServicioNoDisponibleException;
import com.devsenior.sillon.ia.dto.RespuestaIa;
import com.devsenior.sillon.ia.proveedor.ProveedorIa;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AsistenteIaService {

    private final List<ProveedorIa> proveedores;

    public AsistenteIaService(List<ProveedorIa> proveedores) {
        this.proveedores = proveedores;
    }

    public RespuestaIa generar(String instruccionesSistema, String mensajeUsuario) {
        for (ProveedorIa proveedor : proveedores) {
            try {
                return proveedor.generar(instruccionesSistema, mensajeUsuario);
            } catch (Exception ignored) {
                // El proveedor ya registró el ERROR en log; intenta el siguiente
            }
        }
        throw new ServicioNoDisponibleException("El servicio de IA no está disponible en este momento");
    }
}

