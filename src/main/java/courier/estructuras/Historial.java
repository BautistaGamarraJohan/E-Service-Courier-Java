package courier.estructuras;

import courier.modelo.Operacion;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public final class Historial {
    private final Deque<Operacion> operaciones = new ArrayDeque<>();

    public void apilar(Operacion operacion) {
        operaciones.push(operacion);
    }

    public List<Operacion> listar() {
        return List.copyOf(operaciones);
    }
}
