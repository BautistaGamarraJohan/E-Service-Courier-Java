package courier.interfaz;

import courier.modelo.Operacion;
import courier.modelo.Pedido;
import courier.servicio.GestorPedidos;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;

public final class VentanaPrincipal extends JFrame {
    private static final Color AZUL = new Color(13, 41, 78);
    private static final Color FONDO = new Color(233, 240, 249);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final GestorPedidos gestor = new GestorPedidos();
    private final JTextField destinatario = new JTextField();
    private final JTextField direccion = new JTextField();
    private final JTextField descripcion = new JTextField();
    private final JLabel conteos = new JLabel();
    private final JLabel mensaje = new JLabel("Listo");
    private final DefaultTableModel pedidos = modelo("ID", "Destinatario", "Dirección", "Estado");
    private final DefaultTableModel recientes = modelo("Fecha / Hora", "Operación", "Detalle");
    private final JTable tablaPedidos = tabla(pedidos);
    private final JButton entregar = new JButton("Confirmar entrega");

    private VentanaPrincipal() {
        super("E-Service Courier — Sistema de Gestión de Envíos");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(880, 630));
        setSize(1170, 740);
        setLocationRelativeTo(null);

        JPanel raiz = new JPanel(new BorderLayout());
        setContentPane(raiz);
        raiz.add(banda("E-Service Courier     |     Tu envío, nuestra prioridad"), BorderLayout.NORTH);
        raiz.add(barraLateral(), BorderLayout.WEST);

        JPanel cuerpo = new JPanel(new BorderLayout(0, 12));
        cuerpo.setBackground(FONDO);
        cuerpo.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        raiz.add(cuerpo, BorderLayout.CENTER);

        JPanel registro = seccion("Registrar nuevo pedido");
        JPanel campos = new JPanel(new GridLayout(1, 4, 10, 0));
        campos.add(campo("Destinatario *", destinatario));
        campos.add(campo("Dirección *", direccion));
        campos.add(campo("Descripción *", descripcion));
        campos.add(boton("Registrar pedido", this::registrar));
        registro.add(campos, BorderLayout.CENTER);
        cuerpo.add(registro, BorderLayout.NORTH);

        JPanel lista = seccion("Pedidos en cola de entrega");
        tablaPedidos.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable tabla, Object valor,
                    boolean seleccionado, boolean foco, int fila, int columna) {
                super.getTableCellRendererComponent(tabla, valor, seleccionado, foco, fila, columna);
                setHorizontalAlignment(CENTER);
                setFont(getFont().deriveFont(Font.BOLD));
                if (!seleccionado && valor instanceof Pedido.Estado estado) {
                    setBackground(switch (estado) {
                        case EN_COLA -> new Color(255, 235, 181);
                        case EN_TRANSITO -> new Color(203, 229, 255);
                        case ENTREGADO -> new Color(203, 240, 207);
                    });
                    setForeground(AZUL);
                }
                return this;
            }
        });
        lista.add(new JScrollPane(tablaPedidos), BorderLayout.CENTER);
        JPanel acciones = new JPanel(new GridLayout(3, 1, 0, 8));
        acciones.add(boton("Procesar siguiente envío", this::procesar));
        entregar.addActionListener(e -> confirmarEntrega());
        entregar.setEnabled(false);
        tablaPedidos.getSelectionModel().addListSelectionListener(e -> {
            int fila = tablaPedidos.getSelectedRow();
            entregar.setEnabled(fila >= 0 && pedidos.getValueAt(
                    tablaPedidos.convertRowIndexToModel(fila), 3) == Pedido.Estado.EN_TRANSITO);
        });
        acciones.add(entregar);
        acciones.add(conteos);
        

        JPanel historial = seccion("Historial de operaciones");
        historial.add(new JScrollPane(tabla(recientes)), BorderLayout.CENTER);
        JPanel pie = new JPanel(new BorderLayout());
        pie.add(mensaje, BorderLayout.CENTER);
        pie.add(boton("Ver todo", this::verHistorial), BorderLayout.EAST);
        historial.add(pie, BorderLayout.SOUTH);

        JPanel control = seccion("Control de operaciones");
        control.add(acciones, BorderLayout.CENTER);
        JPanel inferior = new JPanel(new BorderLayout(12, 0));
        inferior.setOpaque(false);
        inferior.add(control, BorderLayout.WEST);
        inferior.add(historial, BorderLayout.CENTER);
        JSplitPane paneles = new JSplitPane(JSplitPane.VERTICAL_SPLIT, lista, inferior);
        paneles.setResizeWeight(0.65);
        paneles.setDividerLocation(310);
        paneles.setBorder(null);
        cuerpo.add(paneles, BorderLayout.CENTER);
        actualizar();
    }

    private void registrar() {
        try {
            Pedido pedido = gestor.registrar(destinatario.getText(), direccion.getText(), descripcion.getText());
            destinatario.setText("");
            direccion.setText("");
            descripcion.setText("");
            destinatario.requestFocusInWindow();
            mensaje.setText("Pedido " + id(pedido) + " registrado.");
            actualizar();
        } catch (IllegalArgumentException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "Datos del pedido",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void procesar() {
        mensaje.setText(gestor.procesarSiguiente()
                .map(pedido -> "Pedido " + id(pedido) + " puesto en tránsito.")
                .orElse("No hay pedidos pendientes."));
        actualizar();
    }

    private void confirmarEntrega() {
        int fila = tablaPedidos.convertRowIndexToModel(tablaPedidos.getSelectedRow());
        Pedido pedido = gestor.entregar(Integer.parseInt(pedidos.getValueAt(fila, 0).toString()));
        mensaje.setText("Entrega del pedido " + id(pedido) + " confirmada.");
        actualizar();
    }

    private void actualizar() {
        pedidos.setRowCount(0);
        int pendientes = 0;
        int transito = 0;
        for (Pedido pedido : gestor.consultarPedidos()) {
            pedidos.addRow(new Object[]{id(pedido), pedido.destinatario(), pedido.direccion(), pedido.estado()});
            if (pedido.estado() == Pedido.Estado.EN_COLA) {
                pendientes++;
            } else if (pedido.estado() == Pedido.Estado.EN_TRANSITO) {
                transito++;
            }
        }
        long entregadosHoy = gestor.consultarHistorial().stream()
                .filter(op -> op.tipo() == Operacion.Tipo.ENTREGA
                        && op.fechaHora().toLocalDate().equals(LocalDate.now()))
                .count();
        conteos.setText("En cola: " + pendientes + "    En tránsito: " + transito
                + "    Entregados hoy: " + entregadosHoy);
        recientes.setRowCount(0);
        gestor.consultarHistorial().stream().limit(5).forEach(op -> recientes.addRow(new Object[]{
            FECHA.format(op.fechaHora()), op.tipo(),
            id(op.pedido()) + " · " + op.pedido().destinatario() + " · " + op.detalle()
        }));
    }

    private void verHistorial() {
        JDialog ventana = new JDialog(this, "Historial de operaciones", true);
        ventana.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JPanel contenido = new JPanel(new BorderLayout(10, 10));
        contenido.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        ventana.setContentPane(contenido);
        JTextField desde = new JTextField();
        JTextField hasta = new JTextField();
        JTextField buscar = new JTextField();
        JComboBox<Object> tipo = new JComboBox<>(new Object[]{"Todas",
            Operacion.Tipo.REGISTRO, Operacion.Tipo.PROCESAMIENTO, Operacion.Tipo.ENTREGA});
        JPanel filtros = new JPanel(new GridLayout(1, 4, 10, 0));
        filtros.add(campo("Desde (AAAA-MM-DD)", desde));
        filtros.add(campo("Hasta (AAAA-MM-DD)", hasta));
        filtros.add(campo("Tipo de operación", tipo));
        filtros.add(campo("Buscar ID, destinatario o dirección", buscar));
        JPanel encabezado = new JPanel(new BorderLayout(0, 10));
        encabezado.add(banda("Historial de operaciones"), BorderLayout.NORTH);
        encabezado.add(filtros, BorderLayout.CENTER);
        contenido.add(encabezado, BorderLayout.NORTH);

        DefaultTableModel resultados = modelo("Fecha / Hora", "Operación", "ID",
                "Destinatario", "Dirección", "Detalle");
        contenido.add(new JScrollPane(tabla(resultados)), BorderLayout.CENTER);
        JLabel total = new JLabel();
        JPanel pie = new JPanel(new BorderLayout());
        pie.add(total, BorderLayout.CENTER);
        pie.add(boton("Cerrar", ventana::dispose), BorderLayout.EAST);
        contenido.add(pie, BorderLayout.SOUTH);

        Runnable filtrar = () -> {
            resultados.setRowCount(0);
            try {
                LocalDate inicio = fechaOpcional(desde.getText());
                LocalDate fin = fechaOpcional(hasta.getText());
                if (inicio != null && fin != null && inicio.isAfter(fin)) {
                    total.setText("La fecha inicial debe ser anterior o igual a la final.");
                    return;
                }
                String texto = buscar.getText().strip().toLowerCase(Locale.ROOT);
                gestor.consultarHistorial().stream()
                        .filter(op -> tipo.getSelectedIndex() == 0 || op.tipo() == tipo.getSelectedItem())
                        .filter(op -> inicio == null || !op.fechaHora().toLocalDate().isBefore(inicio))
                        .filter(op -> fin == null || !op.fechaHora().toLocalDate().isAfter(fin))
                        .filter(op -> coincide(op.pedido(), texto))
                        .forEach(op -> resultados.addRow(new Object[]{FECHA.format(op.fechaHora()),
                            op.tipo(), id(op.pedido()), op.pedido().destinatario(),
                            op.pedido().direccion(), op.detalle()}));
                total.setText("Total de registros: " + resultados.getRowCount());
            } catch (DateTimeParseException error) {
                total.setText("Escribe las fechas en formato AAAA-MM-DD.");
            }
        };
        tipo.addActionListener(e -> filtrar.run());
        for (JTextField campo : new JTextField[]{desde, hasta, buscar}) {
            campo.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { filtrar.run(); }
                public void removeUpdate(DocumentEvent e) { filtrar.run(); }
                public void changedUpdate(DocumentEvent e) { filtrar.run(); }
            });
        }
        filtrar.run();
        ventana.setSize(900, 460);
        ventana.setLocationRelativeTo(this);
        ventana.setVisible(true);
    }

    private static LocalDate fechaOpcional(String texto) {
        return texto.isBlank() ? null : LocalDate.parse(texto.strip());
    }

    private static boolean coincide(Pedido pedido, String texto) {
        return id(pedido).contains(texto)
                || pedido.destinatario().toLowerCase(Locale.ROOT).contains(texto)
                || pedido.direccion().toLowerCase(Locale.ROOT).contains(texto);
    }

    private static String id(Pedido pedido) {
        return String.format("%03d", pedido.id());
    }

    private JPanel barraLateral() {
        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBackground(AZUL);
        menu.setPreferredSize(new Dimension(155, 0));
        JButton inicio = boton("Inicio", destinatario::requestFocusInWindow);
        inicio.setMaximumSize(new Dimension(155, 42));
        menu.add(inicio);
        JButton historial = boton("Historial", this::verHistorial);
        historial.setMaximumSize(new Dimension(155, 42));
        menu.add(historial);
        return menu;
    }

    private static JPanel seccion(String titulo) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(198, 213, 234)),
                BorderFactory.createEmptyBorder(9, 11, 9, 11)));
        JLabel etiqueta = new JLabel(titulo);
        etiqueta.setForeground(AZUL);
        etiqueta.setFont(etiqueta.getFont().deriveFont(Font.BOLD));
        panel.add(etiqueta, BorderLayout.NORTH);
        return panel;
    }

    private static JLabel banda(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setOpaque(true);
        etiqueta.setBackground(AZUL);
        etiqueta.setForeground(Color.WHITE);
        etiqueta.setFont(etiqueta.getFont().deriveFont(Font.BOLD, 22f));
        etiqueta.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        return etiqueta;
    }

    private static JPanel campo(String titulo, JComponent entrada) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        JLabel etiqueta = new JLabel(titulo);
        etiqueta.setForeground(AZUL);
        etiqueta.setFont(etiqueta.getFont().deriveFont(Font.BOLD));
        panel.add(etiqueta, BorderLayout.NORTH);
        panel.add(entrada, BorderLayout.CENTER);
        return panel;
    }

    private static JButton boton(String texto, Runnable accion) {
        JButton boton = new JButton(texto);
        boton.setBackground(new Color(23, 97, 188));
        boton.setForeground(Color.WHITE);
        boton.addActionListener(e -> accion.run());
        return boton;
    }

    private static DefaultTableModel modelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
    }

    private static JTable tabla(DefaultTableModel modelo) {
        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(28);
        tabla.getTableHeader().setBackground(new Color(220, 234, 250));
        tabla.getTableHeader().setForeground(AZUL);
        tabla.getTableHeader().setFont(tabla.getTableHeader().getFont().deriveFont(Font.BOLD));
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return tabla;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VentanaPrincipal().setVisible(true));
    }
}


