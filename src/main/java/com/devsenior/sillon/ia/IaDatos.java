package com.devsenior.sillon.ia;

import static com.devsenior.sillon.compartido.Fallos.fallo;

import com.devsenior.sillon.chat.Solicitud;
import com.devsenior.sillon.compartido.Datos;
import com.devsenior.sillon.compartido.Entradas;
import com.devsenior.sillon.salon.Catalogos;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.seguridad.Actor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class IaDatos {
    final Datos datos;
    final Clock reloj;

    IaDatos(Datos datos, Clock reloj) {
        this.datos = datos;
        this.reloj = reloj;
    }

    public record Contexto(
            long salonId, String salon, String cliente, String telefono, String hechos) {}

    public static void verificar(Salon s) {
        if (s.getCreditosIa() < 1) throw fallo(402, "El salón agotó sus créditos de IA");
    }

    @Transactional(readOnly = true)
    public Contexto paraCita(Actor a, long id) {
        var c = datos.cita(a, id);
        var s = c.getSalon();
        if (!Set.of("CONFIRMADA", "PENDIENTE").contains(c.getEstado()))
            throw fallo(409, "La cita ya terminó o se canceló");
        verificar(s);
        var co = Locale.forLanguageTag("es-CO");
        String hora =
                c.getInicio().format(DateTimeFormatter.ofPattern("h:mm", co))
                        + (c.getInicio().getHour() < 12 ? " a. m." : " p. m.");
        String fecha = c.getInicio().format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", co));
        String hechos =
                String.join(
                        "\n",
                        "Servicio: "
                                + c.getServicio().getNombre()
                                + " ("
                                + java.time.Duration.between(c.getInicio(), c.getFin()).toMinutes()
                                + " minutos)",
                        "Estilista: " + c.getEstilista().getNombre(),
                        "Fecha: " + fecha,
                        "Hora: " + hora,
                        "Valor: $" + NumberFormat.getIntegerInstance(co).format(c.getPrecio()),
                        "Dirección: " + s.getDireccion() + ", " + s.getCiudad());
        return new Contexto(
                s.getId(),
                s.getNombre(),
                c.getCliente().getNombre(),
                c.getCliente().getTelefono(),
                hechos);
    }

    public record ContextoChat(
            long salonId, String salon, String direccion, String horario, String catalogo) {}

    @Transactional(readOnly = true)
    public ContextoChat paraChat(String slug) {
        var s = datos.publico(slug, false);
        verificar(s);
        var catalogo =
                datos.servicios(s).stream()
                        .filter(x -> x.getActivo())
                        .map(x ->
                                x.getNombre()
                                        + " · "
                                        + x.getDuracionMinutos()
                                        + " minutos · "
                                        + x.getPrecio().toPlainString()
                                        + " COP")
                        .collect(Collectors.joining("\n"));
        return new ContextoChat(
                s.getId(),
                s.getNombre(),
                s.getDireccion() + ", " + s.getCiudad(),
                Catalogos.horario(s),
                catalogo);
    }

    public int consumir(long id) {
        int filas =
                datos.em()
                        .createQuery(
                                "update Salon set creditosIa=creditosIa-1 where id=:id and"
                                    + " creditosIa>0")
                        .setParameter("id", id)
                        .executeUpdate();
        if (filas == 0) throw fallo(402, "El salón agotó sus créditos de IA");
        return datos.salon(id, false).getCreditosIa();
    }

    public Long solicitar(long salonId, Entradas.Chat p, String motivo) {
        var s = new Solicitud();
        s.setSalon(datos.salon(salonId, false));
        s.setClienteNombre(p.clienteNombre());
        s.setClienteContacto(p.clienteContacto());
        String mensajes =
                p.historial().stream()
                        .filter(t -> t.rol().equals("CLIENTE"))
                        .map(Entradas.Turno::texto)
                        .collect(Collectors.joining("\n"));
        s.setMensajes(mensajes.substring(0, Math.min(mensajes.length(), 2000)));
        s.setMotivo(motivo);
        s.setFechaCreacion(LocalDateTime.now(reloj));
        datos.em().persist(s);
        return s.getId();
    }
}
