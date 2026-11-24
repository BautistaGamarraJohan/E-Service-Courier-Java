package courier.servicio;

import courier.estructuras.ColaPedidos;
import courier.estructuras.Historial;
import courier.modelo.Operacion;
import courier.modelo.Pedido;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

public final class GestorPedidos {
    private final ColaPedidos cola = new ColaPedidos();
    private final Historial historial = new Historial();
    private final TreeMap<Integer, Pedido> pedidos = new TreeMap<>();
    private int siguienteId = 1;

    public Pedido registrar(String destinatario, String direccion, String descripcion) {
        if (destinatario.isBlank() || direccion.isBlank() || descripcion.isBlank()) {
            throw new IllegalArgumentException("Completa destinatario, dirección y descripción.");
        }
        Pedido pedido = new Pedido(siguienteId++, destinatario.strip(), direccion.strip(),
                descripcion.strip(), Pedido.Estado.EN_COLA);
        cola.encolar(pedido);
        guardar(pedido, Operacion.Tipo.REGISTRO, "Pedido registrado en el sistema.");
        return pedido;
    }

    public Optional<Pedido> procesarSiguiente() {
        return cola.desencolar().map(pendiente -> {
            Pedido pedido = pendiente.conEstado(Pedido.Estado.EN_TRANSITO);
            guardar(pedido, Operacion.Tipo.PROCESAMIENTO, "Pedido puesto en tránsito.");
            return pedido;
        });
    }

    public Pedido entregar(int id) {
        Pedido actual = pedidos.get(id);
        if (actual == null || actual.estado() != Pedido.Estado.EN_TRANSITO) {
            throw new IllegalArgumentException("Selecciona un pedido en tránsito.");
        }
        Pedido pedido = actual.conEstado(Pedido.Estado.ENTREGADO);
        guardar(pedido, Operacion.Tipo.ENTREGA, "Entrega confirmada.");
        return pedido;
    }

    public List<Pedido> consultarPedidos() {
        return List.copyOf(pedidos.values());
    }

    public List<Operacion> consultarHistorial() {
        return historial.listar();
    }

    private void guardar(Pedido pedido, Operacion.Tipo tipo, String detalle) {
        pedidos.put(pedido.id(), pedido);
        historial.apilar(new Operacion(LocalDateTime.now(), tipo, pedido, detalle));
    }
}
