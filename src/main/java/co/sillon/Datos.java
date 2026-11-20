package co.sillon;

import jakarta.persistence.*;
import java.util.List;
import org.springframework.stereotype.Repository;
import static co.sillon.Fallos.fallo;

@Repository
class Datos {
    @PersistenceContext private EntityManager em;
    EntityManager em() { return em; }

    Salon salon(long id, boolean escritura) {
        Salon s = em.find(Salon.class, id, escritura ? LockModeType.PESSIMISTIC_WRITE : LockModeType.NONE);
        if (s == null) throw fallo(404,"Salón desconocido");
        return s;
    }
    Salon publico(String slug, boolean escritura) {
        var q = em.createQuery("from Salon where slug=:slug", Salon.class).setParameter("slug",slug);
        if (escritura) q.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        return q.getResultList().stream().findFirst().orElseThrow(() -> fallo(404,"Salón desconocido"));
    }
    List<Servicio> servicios(Salon s) {
        return em.createQuery("from Servicio where salon=:s order by id",Servicio.class)
                .setParameter("s",s).getResultList();
    }
    List<Usuario> estilistas(Salon s) {
        return em.createQuery("from Usuario where salon=:s and rol='ESTILISTA' order by id",Usuario.class)
                .setParameter("s",s).getResultList();
    }
    Servicio servicio(Salon s, long id) {
        return em.createQuery("from Servicio where salon=:s and id=:id",Servicio.class)
            .setParameter("s",s).setParameter("id",id).getResultList().stream().findFirst()
            .orElseThrow(() -> fallo(404,"Servicio desconocido"));
    }
    Usuario estilista(Salon s, long id) {
        return em.createQuery("from Usuario where salon=:s and id=:id and rol='ESTILISTA'",Usuario.class)
            .setParameter("s",s).setParameter("id",id).getResultList().stream().findFirst()
            .orElseThrow(() -> fallo(404,"Estilista desconocido"));
    }
    Cita cita(Actor actor, long id) {
        var q = em.createQuery("from Cita where salon.id=:salon and id=:id",Cita.class)
                .setParameter("salon",actor.salon()).setParameter("id",id);
        return q.getResultList().stream().filter(c -> actor.dueno() || c.estilista.id == actor.id())
                .findFirst().orElseThrow(() -> fallo(404,"Cita desconocida"));
    }
}
