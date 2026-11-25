package com.devsenior.sillon.cita;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.cliente.Cliente;
import com.devsenior.sillon.cliente.ClienteServicio;
import com.devsenior.sillon.compartido.AccionNoPermitidaException;
import com.devsenior.sillon.compartido.PasarelaException;
import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.ia.AsistenteIa;
import com.devsenior.sillon.ia.BibliotecaPrompts;
import com.devsenior.sillon.ia.CreditosIaServicio;
import com.devsenior.sillon.ia.LectorJson;
import com.devsenior.sillon.ia.RespuestaIa;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepository;

import tools.jackson.databind.JsonNode;

@Service
public class CitaServicio {

    private final CitaRepository citaRepository;
    private final SalonRepository salonRepository;
    private final ServicioRepository servicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteServicio clienteServicio;
    private final ReglasAgenda reglasAgenda;
    private final CitaMapper citaMapper;
    private final Clock reloj;
    private final AsistenteIa asistenteIa;
    private final BibliotecaPrompts bibliotecaPrompts;
    private final LectorJson lectorJson;
    private final CreditosIaServicio creditosIaServicio;

    public CitaServicio(CitaRepository citaRepository,
                        SalonRepository salonRepository,
                        ServicioRepository servicioRepository,
                        UsuarioRepository usuarioRepository,
                        ClienteServicio clienteServicio,
                        ReglasAgenda reglasAgenda,
                        CitaMapper citaMapper,
                        Clock reloj,
                        AsistenteIa asistenteIa,
                        BibliotecaPrompts bibliotecaPrompts,
                        LectorJson lectorJson,
                        CreditosIaServicio creditosIaServicio) {
        this.citaRepository = citaRepository;
        this.salonRepository = salonRepository;
        this.servicioRepository = servicioRepository;
        this.usuarioRepository = usuarioRepository;
        this.clienteServicio = clienteServicio;
        this.reglasAgenda = reglasAgenda;
        this.citaMapper = citaMapper;
        this.reloj = reloj;
        this.asistenteIa = asistenteIa;
        this.bibliotecaPrompts = bibliotecaPrompts;
        this.lectorJson = lectorJson;
        this.creditosIaServicio = creditosIaServicio;
    }

    /** Cita creada directamente por el personal del salon: nace CONFIRMADA y con origen SALON. */
    @Transactional
    public CitaRespuesta crearPorSalon(Long salonId, CitaCreacionPeticion peticion) {
        return agendar(salonId, peticion, EstadoCita.CONFIRMADA, OrigenCita.SALON);
    }

    /** Reserva en linea por un cliente: nace PENDIENTE y con origen EN_LINEA (Fase 5). */
    @Transactional
    public CitaRespuesta reservarEnLinea(Long salonId, CitaCreacionPeticion peticion) {
        return agendar(salonId, peticion, EstadoCita.PENDIENTE, OrigenCita.EN_LINEA);
    }

    /**
     * Unico metodo privado para agendar citas, con orden de validacion estricto:
     * 1. Servicio existe en el salon y esta activo (404 / 409).
     * 2. Estilista existe en el salon, esta activo y ofrece el servicio (404 / 409).
     * 3. Calcula fin con la duracion del servicio.
     * 4. La cita no esta en el pasado (con el reloj del proyecto, 409).
     * 5. Cumple el horario del salon (apertura, cierre, domingos, 409).
     * 6. No se cruza con otra cita del estilista que ocupe agenda (409).
     * 7. Busca o crea el cliente, copia el precio y guarda con estado y origen.
     */
    private CitaRespuesta agendar(Long salonId, CitaCreacionPeticion peticion, EstadoCita estadoInicial, OrigenCita origen) {
        // 1. Servicio existe en el salon y esta activo
        Servicio servicio = servicioRepository.findByIdAndSalonId(peticion.servicioId(), salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado o no pertenece a su salón"));
        if (!servicio.isActivo()) {
            throw new ReglaNegocioException("El servicio seleccionado no está activo.");
        }

        // 2. Estilista existe en el salon, esta activo y ofrece el servicio
        Usuario estilista = usuarioRepository.findByIdAndSalonIdAndRol(peticion.estilistaId(), salonId, Rol.ESTILISTA)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estilista no encontrado o no pertenece a su salón"));
        if (!estilista.isActivo()) {
            throw new ReglaNegocioException("El estilista seleccionado no está activo.");
        }
        if (!estilista.ofreceServicio(servicio.getId())) {
            throw new ReglaNegocioException("El estilista no ofrece el servicio seleccionado.");
        }

        // 3. Calcula fin
        LocalDateTime fin = peticion.inicio().plusMinutes(servicio.getDuracionMinutos());

        // 4. No esta en el pasado (con el reloj del proyecto)
        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (peticion.inicio().isBefore(ahora)) {
            throw new ReglaNegocioException("No se puede agendar una cita en el pasado.");
        }

        // 5. Cumple el horario del salon
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));
        reglasAgenda.validarHorario(peticion.inicio(), fin, salon.getConfiguracion());

        // 6. Sin cruces con citas que ocupan agenda (PENDIENTE y CONFIRMADA)
        List<Cita> cruces = citaRepository.findCruce(
                salonId,
                estilista.getId(),
                EstadoCita.estadosQueOcupanAgenda(),
                peticion.inicio(),
                fin);
        if (!cruces.isEmpty()) {
            throw new ReglaNegocioException("El estilista ya tiene una cita agendada en ese horario.");
        }

        // 7. Busca o crea el cliente, copia el precio del momento y guarda
        Cliente cliente = clienteServicio.buscarOCrear(salonId, peticion.clienteNombre(), peticion.clienteTelefono());

        Cita cita = new Cita();
        cita.setSalon(salon);
        cita.setCliente(cliente);
        cita.setEstilista(estilista);
        cita.setServicio(servicio);
        cita.setInicio(peticion.inicio());
        cita.setFin(fin);
        cita.setPrecio(servicio.getPrecio());
        cita.setEstado(estadoInicial);
        cita.setOrigen(origen);
        cita.setNota(peticion.nota() != null ? peticion.nota().trim() : null);
        cita.setFechaCreacion(ahora);
        cita.setFechaActualizacion(ahora);

        Cita guardada = citaRepository.save(cita);
        return citaMapper.aRespuesta(guardada);
    }

    /**
     * Listado del dia:
     * El dueno ve todas las citas del salon.
     * El estilista solo ve las suyas.
     */
    @Transactional(readOnly = true)
    public List<CitaRespuesta> listarPorDia(UsuarioPrincipal usuario, LocalDate fecha) {
        LocalDate fechaConsulta = (fecha != null) ? fecha : LocalDate.now(reloj);
        LocalDateTime inicioDia = fechaConsulta.atStartOfDay();
        LocalDateTime finDia = fechaConsulta.plusDays(1).atStartOfDay();

        List<Cita> citas;
        if (usuario.getRol() == Rol.DUENO) {
            citas = citaRepository.findBySalonIdAndInicioBetweenOrderByInicioAsc(
                    usuario.getSalonId(), inicioDia, finDia);
        } else {
            citas = citaRepository.findBySalonIdAndEstilistaIdAndInicioBetweenOrderByInicioAsc(
                    usuario.getSalonId(), usuario.getId(), inicioDia, finDia);
        }

        return citas.stream()
                .map(citaMapper::aRespuesta)
                .toList();
    }

    /**
     * Cambiar estado de una cita:
     * - Si es estilista: solo sus citas (404 si es de otro) y solo a COMPLETADA o NO_ASISTIO (403 otro destino).
     * - Si es dueno: cualquier transicion valida.
     * - Transicion invalida segun la maquina de estados: 409.
     * - Se persiste con saveAndFlush para refrescar fechaActualizacion.
     */
    @Transactional
    public CitaRespuesta cambiarEstado(UsuarioPrincipal usuario, Long citaId, EstadoCita nuevoEstado) {
        Cita cita = citaRepository.findByIdAndSalonId(citaId, usuario.getSalonId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada o pertenece a otro salón"));

        if (usuario.getRol() == Rol.ESTILISTA) {
            if (!cita.getEstilista().getId().equals(usuario.getId())) {
                throw new RecursoNoEncontradoException("Cita no encontrada o pertenece a otro estilista");
            }
            if (nuevoEstado != EstadoCita.COMPLETADA && nuevoEstado != EstadoCita.NO_ASISTIO) {
                throw new AccionNoPermitidaException("El estilista solo puede marcar citas como COMPLETADA o NO_ASISTIO.");
            }
        }

        if (!cita.getEstado().puedeTransicionarA(nuevoEstado)) {
            throw new ReglaNegocioException("Transición de estado no permitida de "
                    + cita.getEstado() + " a " + nuevoEstado + ".");
        }

        cita.setEstado(nuevoEstado);
        cita.setFechaActualizacion(LocalDateTime.now(reloj));
        Cita guardada = citaRepository.saveAndFlush(cita);

        return citaMapper.aRespuesta(guardada);
    }

    /**
     * Genera un borrador de recordatorio con IA para WhatsApp con los datos verificados de la cita.
     * Orden estricto según la especificación:
     * 1. Validar que la cita exista en el salón (404) y esté en estado PENDIENTE o CONFIRMADA (409).
     * 2. Verificar créditos (402 si 0 créditos, la IA no es llamada).
     * 3. Cargar prompt recordatorio.txt y llamar a la IA (fuera de transacción de base de datos).
     * 4. Consumir crédito de forma atómica.
     * 5. Leer y validar JSON (clave mensaje, máx 700 caracteres, 502 si falla).
     * 6. Devolver DTO RecordatorioRespuesta.
     */
    public RecordatorioRespuesta generarRecordatorio(Long citaId, Long salonId) {
        Cita cita = citaRepository.findByIdAndSalonId(citaId, salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada o pertenece a otro salón"));

        if (cita.getEstado() != EstadoCita.PENDIENTE && cita.getEstado() != EstadoCita.CONFIRMADA) {
            throw new ReglaNegocioException("Solo se pueden generar recordatorios para citas en estado PENDIENTE o CONFIRMADA.");
        }

        creditosIaServicio.verificarCreditos(cita.getSalon());

        String direccion = cita.getSalon().getDireccion() != null ? cita.getSalon().getDireccion().trim() : "";
        String ciudad = cita.getSalon().getCiudad() != null ? cita.getSalon().getCiudad().trim() : "";
        String ubicacion = direccion;
        if (!ciudad.isEmpty()) {
            ubicacion = ubicacion.isEmpty() ? ciudad : ubicacion + ", " + ciudad;
        }

        String datosVerificados = String.format(
                "Servicio: %s (%d minutos)\n" +
                "Estilista: %s\n" +
                "Fecha: %s\n" +
                "Hora: %s\n" +
                "Valor: %s\n" +
                "Dirección: %s",
                cita.getServicio().getNombre(),
                cita.getServicio().getDuracionMinutos(),
                cita.getEstilista().getNombre(),
                formatearFechaEspanol(cita.getInicio().toLocalDate()),
                formatearHoraEspanol(cita.getInicio().toLocalTime()),
                formatearPrecio(cita.getPrecio()),
                ubicacion
        );

        String instrucciones = bibliotecaPrompts.obtenerPrompt("recordatorio", Map.of(
                "salon", cita.getSalon().getNombre() != null ? cita.getSalon().getNombre() : "",
                "cliente", cita.getCliente().getNombre() != null ? cita.getCliente().getNombre() : "",
                "datos", datosVerificados
        ));

        RespuestaIa respuestaIa = asistenteIa.completar(instrucciones, "Genera el recordatorio para el cliente.");

        int creditosRestantes = creditosIaServicio.consumirCredito(salonId);

        JsonNode nodo = lectorJson.leerJson(respuestaIa.texto(), "mensaje");
        String mensaje = nodo.get("mensaje").asString();

        if (mensaje == null || mensaje.isBlank()) {
            throw new PasarelaException("El mensaje del recordatorio no puede estar vacío.");
        }
        if (mensaje.length() > 700) {
            throw new PasarelaException("El mensaje del recordatorio supera el límite de 700 caracteres.");
        }

        return new RecordatorioRespuesta(
                cita.getId(),
                cita.getCliente().getTelefono(),
                mensaje,
                creditosRestantes,
                respuestaIa.factura()
        );
    }

    private String formatearPrecio(Integer precio) {
        if (precio == null) {
            return "$0";
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.of("es", "CO"));
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        return "$" + df.format(precio);
    }

    private String formatearFechaEspanol(LocalDate fecha) {
        return DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.of("es", "CO"))
                .format(fecha)
                .toLowerCase(Locale.of("es", "CO"));
    }

    private String formatearHoraEspanol(LocalTime hora) {
        return DateTimeFormatter.ofPattern("h:mm a", Locale.of("es", "CO"))
                .format(hora)
                .replace('\u202F', ' ')
                .replace('\u00A0', ' ');
    }
}
