package courier.estructuras;

import courier.modelo.Pedido;
import java.util.LinkedList;
import java.util.Optional;

public final class ColaPedidos {
    private final LinkedList<Pedido> pendientes = new LinkedList<>();

    public void encolar(Pedido pedido) {
        pendientes.offerLast(pedido);
    }

    public Optional<Pedido> desencolar() {
        return Optional.ofNullable(pendientes.pollFirst());
    }
}
