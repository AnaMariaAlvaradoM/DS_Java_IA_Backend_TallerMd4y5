package co.sillon;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static co.sillon.Fallos.fallo;

@Service
@Transactional
class IaDatos {
    final Datos datos;
    final Clock reloj;
    IaDatos(Datos datos,Clock reloj) { this.datos=datos; this.reloj=reloj; }
    record Contexto(long salonId,String salon,String cliente,String telefono,String hechos) {}
    static void verificar(Salon s) {
        if (s.creditosIa<1) throw fallo(402,"El salón agotó sus créditos de IA");
    }
    @Transactional(readOnly=true)
    Contexto paraCita(Actor a,long id) {
        var c=datos.cita(a,id); var s=c.salon;
        if (!Set.of("CONFIRMADA","PENDIENTE").contains(c.estado)) throw fallo(409,"La cita ya terminó o se canceló");
        verificar(s);
        var co=Locale.forLanguageTag("es-CO");
        String hora=c.inicio.format(DateTimeFormatter.ofPattern("h:mm",co))+(c.inicio.getHour()<12 ? " a. m." : " p. m.");
        String fecha=c.inicio.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM",co));
        String hechos=String.join("\n","Servicio: "+c.servicio.nombre+" ("+java.time.Duration.between(c.inicio,c.fin).toMinutes()+" minutos)",
                "Estilista: "+c.estilista.nombre,"Fecha: "+fecha,"Hora: "+hora,"Valor: $"+NumberFormat.getIntegerInstance(co).format(c.precio),
                "Dirección: "+s.direccion+", "+s.ciudad);
        return new Contexto(s.id,s.nombre,c.cliente.nombre,c.cliente.telefono,hechos);
    }
    record ContextoChat(long salonId,String salon,String direccion,String horario,String catalogo) {}
    @Transactional(readOnly=true)
    ContextoChat paraChat(String slug) {
        var s=datos.publico(slug,false); verificar(s);
        var catalogo=datos.servicios(s).stream().filter(x -> x.activo).map(x -> x.nombre+" · "+x.duracionMinutos+" minutos · "+x.precio.toPlainString()+" COP")
                .collect(Collectors.joining("\n"));
        return new ContextoChat(s.id,s.nombre,s.direccion+", "+s.ciudad,Catalogos.horario(s),catalogo);
    }
    int consumir(long id) {
        int filas=datos.em().createQuery("update Salon set creditosIa=creditosIa-1 where id=:id and creditosIa>0")
                .setParameter("id",id).executeUpdate();
        if (filas==0) throw fallo(402,"El salón agotó sus créditos de IA");
        return datos.salon(id,false).creditosIa;
    }
    Long solicitar(long salonId,Entradas.Chat p,String motivo) {
        var s=new Solicitud();
        s.salon=datos.salon(salonId,false); s.clienteNombre=p.clienteNombre(); s.clienteContacto=p.clienteContacto();
        String mensajes=p.historial().stream().filter(t -> t.rol().equals("CLIENTE")).map(Entradas.Turno::texto).collect(Collectors.joining("\n"));
        s.mensajes=mensajes.substring(0,Math.min(mensajes.length(),2000)); s.motivo=motivo; s.fechaCreacion=LocalDateTime.now(reloj);
        datos.em().persist(s); return s.id;
    }
}
