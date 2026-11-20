package com.devsenior.sillon.compartido;

import static com.devsenior.sillon.compartido.Fallos.fallo;

import com.devsenior.sillon.cita.Cita;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.seguridad.Actor;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.usuario.Usuario;

import jakarta.persistence.*;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class Datos {
    @PersistenceContext private EntityManager em;

    public EntityManager em() {
        return em;
    }

    public Salon salon(long id, boolean escritura) {
        Salon s =
                em.find(
                        Salon.class,
                        id,
                        escritura ? LockModeType.PESSIMISTIC_WRITE : LockModeType.NONE);
        if (s == null) throw fallo(404, "Salón desconocido");
        return s;
    }

    public Salon publico(String slug, boolean escritura) {
        var q =
                em.createQuery("from Salon where slug=:slug", Salon.class)
                        .setParameter("slug", slug);
        if (escritura) q.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        return q.getResultList().stream()
                .findFirst()
                .orElseThrow(() -> fallo(404, "Salón desconocido"));
    }

    public List<Servicio> servicios(Salon s) {
        return em.createQuery("from Servicio where salon=:s order by id", Servicio.class)
                .setParameter("s", s)
                .getResultList();
    }

    public List<Usuario> estilistas(Salon s) {
        return em.createQuery(
                        "from Usuario where salon=:s and rol=ESTILISTA order by id", Usuario.class)
                .setParameter("s", s)
                .getResultList();
    }

    public Servicio servicio(Salon s, long id) {
        return em
                .createQuery("from Servicio where salon=:s and id=:id", Servicio.class)
                .setParameter("s", s)
                .setParameter("id", id)
                .getResultList()
                .stream()
                .findFirst()
                .orElseThrow(() -> fallo(404, "Servicio desconocido"));
    }

    public Usuario estilista(Salon s, long id) {
        return em
                .createQuery(
                        "from Usuario where salon=:s and id=:id and rol=ESTILISTA", Usuario.class)
                .setParameter("s", s)
                .setParameter("id", id)
                .getResultList()
                .stream()
                .findFirst()
                .orElseThrow(() -> fallo(404, "Estilista desconocido"));
    }

    public Cita cita(Actor actor, long id) {
        var q =
                em.createQuery("from Cita where salon.id=:salon and id=:id", Cita.class)
                        .setParameter("salon", actor.salon())
                        .setParameter("id", id);
        return q.getResultList().stream()
                .filter(c -> actor.dueno() || c.getEstilista().getId() == actor.id())
                .findFirst()
                .orElseThrow(() -> fallo(404, "Cita desconocida"));
    }
}
