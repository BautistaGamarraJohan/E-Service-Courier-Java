package courier.modelo;

import java.time.LocalDateTime;

public record Operacion(LocalDateTime fechaHora, Tipo tipo, Pedido pedido, String detalle) {
    public enum Tipo {
        REGISTRO("Registrado"), PROCESAMIENTO("Procesado"), ENTREGA("Entregado");

        private final String etiqueta;

        Tipo(String etiqueta) {
            this.etiqueta = etiqueta;
        }

        @Override
        public String toString() {
            return etiqueta;
        }
    }
}
