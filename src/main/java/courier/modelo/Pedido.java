package courier.modelo;

public record Pedido(int id, String destinatario, String direccion,
                     String descripcion, Estado estado) {
    public enum Estado {
        EN_COLA("En cola"), EN_TRANSITO("En tránsito"), ENTREGADO("Entregado");

        private final String etiqueta;

        Estado(String etiqueta) {
            this.etiqueta = etiqueta;
        }

        @Override
        public String toString() {
            return etiqueta;
        }
    }

    public Pedido conEstado(Estado nuevoEstado) {
        return new Pedido(id, destinatario, direccion, descripcion, nuevoEstado);
    }
}
