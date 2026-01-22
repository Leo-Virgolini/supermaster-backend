package ar.com.leo.super_master_backend.config;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.*;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Listener global que registra todas las modificaciones a la base de datos.
 * Se registra automáticamente al iniciar la aplicación.
 *
 * Para operaciones masivas, se puede desactivar temporalmente:
 * <pre>
 * AuditEventListener.disable();
 * try {
 *     // operaciones masivas
 * } finally {
 *     AuditEventListener.enable();
 * }
 * </pre>
 */
@Slf4j
@Component
public class AuditEventListener implements PostInsertEventListener,
                                           PostUpdateEventListener,
                                           PostDeleteEventListener {

    // ThreadLocal para desactivar el logging por thread (thread-safe)
    private static final ThreadLocal<Boolean> DISABLED = ThreadLocal.withInitial(() -> false);

    @PersistenceUnit
    private EntityManagerFactory emf;

    /**
     * Desactiva el logging de auditoría para el thread actual.
     */
    public static void disable() {
        DISABLED.set(true);
    }

    /**
     * Reactiva el logging de auditoría para el thread actual.
     */
    public static void enable() {
        DISABLED.set(false);
    }

    /**
     * Verifica si el logging está desactivado para el thread actual.
     */
    public static boolean isDisabled() {
        return DISABLED.get();
    }

    @PostConstruct
    public void registerListeners() {
        SessionFactoryImpl sessionFactory = emf.unwrap(SessionFactoryImpl.class);
        EventListenerRegistry registry = sessionFactory.getServiceRegistry()
                .getService(EventListenerRegistry.class);

        registry.appendListeners(EventType.POST_INSERT, this);
        registry.appendListeners(EventType.POST_UPDATE, this);
        registry.appendListeners(EventType.POST_DELETE, this);

        log.info("AuditEventListener registrado para INSERT, UPDATE y DELETE");
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        if (isDisabled()) return;
        String entityName = event.getEntity().getClass().getSimpleName();
        Object id = event.getId();
        log.info("DB INSERT: {} [id={}]", entityName, id);
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        if (isDisabled()) return;
        String entityName = event.getEntity().getClass().getSimpleName();
        Object id = event.getId();

        // Obtener campos modificados con valores anterior -> nuevo
        int[] dirtyProps = event.getDirtyProperties();
        String[] propNames = event.getPersister().getPropertyNames();
        Object[] oldState = event.getOldState();
        Object[] newState = event.getState();

        List<String> cambios = new ArrayList<>();
        if (dirtyProps != null && oldState != null && newState != null) {
            for (int i : dirtyProps) {
                String campo = propNames[i];
                Object valorAnterior = oldState[i];
                Object valorNuevo = newState[i];
                cambios.add(campo + ": " + valorAnterior + " -> " + valorNuevo);
            }
        }

        log.info("DB UPDATE: {} [id={}] {}", entityName, id, cambios);
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        if (isDisabled()) return;
        String entityName = event.getEntity().getClass().getSimpleName();
        Object id = event.getId();
        log.info("DB DELETE: {} [id={}]", entityName, id);
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }
}
