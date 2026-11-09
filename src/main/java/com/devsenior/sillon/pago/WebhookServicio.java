package com.devsenior.sillon.pago;

import com.devsenior.sillon.compartido.PasarelaException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepositorio;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookServicio {
    private final SalonRepositorio salones;
    private final EventoStripeRepositorio eventos;
    private final Clock clock;
    private final int creditosPro;
    private final int creditosPaquete;

    public WebhookServicio(SalonRepositorio salones, EventoStripeRepositorio eventos, Clock clock,
            @Value("${sillon.creditos.pro}") int creditosPro,
            @Value("${sillon.creditos.paquete}") int creditosPaquete) {
        this.salones = salones;
        this.eventos = eventos;
        this.clock = clock;
        this.creditosPro = creditosPro;
        this.creditosPaquete = creditosPaquete;
    }

    @Transactional
    public WebhookRespuesta procesar(EventoVerificado evento) {
        Optional<Salon> salon = switch (evento.tipo()) {
            case "checkout.session.completed" -> evento.pagado()
                    ? Optional.of(salones.buscarParaActualizar(evento.salonId())
                            .orElseThrow(() -> new PasarelaException("El salón del pago no existe")))
                    : Optional.empty();
            case "customer.subscription.deleted" -> salones
                    .buscarPorSuscripcionParaActualizar(evento.suscripcionId());
            default -> Optional.empty();
        };
        if (eventos.existsById(evento.id())) {
            return new WebhookRespuesta(true, false);
        }
        boolean procesado = false;
        if ("checkout.session.completed".equals(evento.tipo()) && evento.pagado()) {
            Salon destino = salon.orElseThrow();
            if (evento.tipoCompra() == TipoCompra.SUSCRIPCION_PRO) {
                if (evento.suscripcionId() == null || evento.suscripcionId().isBlank()) {
                    throw new PasarelaException("Falta el identificador de la suscripción");
                }
                destino.setPlan(Plan.PRO);
                destino.setStripeSuscripcionId(evento.suscripcionId());
                destino.setCreditosIa(destino.getCreditosIa() + creditosPro);
            } else if (evento.tipoCompra() == TipoCompra.PAQUETE_CREDITOS) {
                destino.setCreditosIa(destino.getCreditosIa() + creditosPaquete);
            } else {
                throw new PasarelaException("Tipo de compra inválido");
            }
            salones.save(destino);
            procesado = true;
        } else if ("customer.subscription.deleted".equals(evento.tipo()) && salon.isPresent()) {
            salon.get().setPlan(Plan.GRATIS);
            salon.get().setStripeSuscripcionId(null);
            salones.save(salon.get());
            procesado = true;
        }
        EventoStripe registro = new EventoStripe();
        registro.setId(evento.id());
        registro.setTipo(evento.tipo());
        registro.setFechaProcesado(LocalDateTime.now(clock));
        eventos.saveAndFlush(registro);
        return new WebhookRespuesta(true, procesado);
    }
}
