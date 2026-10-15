package com.nttdata.ms_tickets;

import com.nttdata.ms_tickets.api.TicketsApiDelegate;
import com.nttdata.ms_tickets.model.Ticket;
import com.nttdata.ms_tickets.model.TicketRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lógica de negocio de la API de tickets.
 * Implementa el delegate generado desde api.yaml (Contract First).
 */
// TODO 11: @Service que implementa la interfaz generada TicketsApiDelegate
@Service
public class TicketApiDelegateImpl implements TicketsApiDelegate {

    // RN-01: límites del título
    private static final int TITULO_MIN = 5;
    private static final int TITULO_MAX = 100;

    // RN-04: horas estimadas según la prioridad
    private static final int HORAS_ALTA = 4;
    private static final int HORAS_MEDIA = 8;
    private static final int HORAS_BAJA = 24;

    // TODO 12: datos iniciales en memoria y secuencia para los ids
    private final List<Ticket> tickets = new ArrayList<>();
    private final AtomicLong secuencia = new AtomicLong();

    public TicketApiDelegateImpl() {
        agregar("No carga el reporte mensual", Ticket.PrioridadEnum.ALTA, Ticket.EstadoEnum.ABIERTO);
        agregar("Error de ortografía en el menú", Ticket.PrioridadEnum.BAJA, Ticket.EstadoEnum.ABIERTO);
        agregar("Lentitud al iniciar sesión", Ticket.PrioridadEnum.MEDIA, Ticket.EstadoEnum.CERRADO);
    }

    // TODO 13 (RN-05): sin filtro devuelve todos; con estado, solo los que coinciden
    @Override
    public ResponseEntity<List<Ticket>> listarTickets(String estado) {
        List<Ticket> resultado;
        synchronized (tickets) {
            resultado = (estado == null || estado.isBlank())
                    ? List.copyOf(tickets)
                    : tickets.stream()
                        .filter(t -> t.getEstado().getValue().equalsIgnoreCase(estado.trim()))
                        .toList();
        }
        return ResponseEntity.ok(resultado);
    }

    // TODO 14 y 15 (RN-06): 200 con el ticket o 404 si no existe
    @Override
    public ResponseEntity<Ticket> obtenerTicket(Long id) {
        synchronized (tickets) {
            return tickets.stream()
                    .filter(t -> t.getId().equals(id))
                    .findFirst()
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        }
    }

    @Override
    public ResponseEntity<Ticket> crearTicket(TicketRequest request) {
        // TODO 16 (RN-01 y RN-02): si los datos no son válidos -> 400
        if (!esValido(request)) {
            return ResponseEntity.badRequest().build();
        }
        // TODO 17 a 19 (RN-03 y RN-04): id correlativo, estado ABIERTO, horas según prioridad y 201
        Ticket.PrioridadEnum prioridad = Ticket.PrioridadEnum.fromValue(request.getPrioridad().getValue());
        Ticket creado = agregar(request.getTitulo().trim(), prioridad, Ticket.EstadoEnum.ABIERTO);
        // TODO 20: el ticket queda en la lista, por lo que GET /api/tickets/{id} lo encuentra después
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    // ------------------------- reglas privadas -------------------------

    private Ticket agregar(String titulo, Ticket.PrioridadEnum prioridad, Ticket.EstadoEnum estado) {
        synchronized (tickets) {
            Ticket ticket = new Ticket(secuencia.incrementAndGet(), titulo, prioridad, estado)
                    .horasEstimadas(horasEstimadas(prioridad));
            tickets.add(ticket);
            return ticket;
        }
    }

    // RN-01 y RN-02
    private boolean esValido(TicketRequest request) {
        if (request == null || request.getTitulo() == null || request.getPrioridad() == null) {
            return false;
        }
        int largo = request.getTitulo().trim().length();
        return largo >= TITULO_MIN && largo <= TITULO_MAX;
    }

    // RN-04
    private int horasEstimadas(Ticket.PrioridadEnum prioridad) {
        return switch (prioridad) {
            case ALTA -> HORAS_ALTA;
            case MEDIA -> HORAS_MEDIA;
            case BAJA -> HORAS_BAJA;
        };
    }
}
