package pucp.edu.pe.glp_final.service;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pucp.edu.pe.glp_final.models.Cliente;
import pucp.edu.pe.glp_final.models.Pedido;
import pucp.edu.pe.glp_final.repository.ClientRepository;
import pucp.edu.pe.glp_final.repository.PedidoRepository;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    private List<Pedido> pedidosCarga;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ClientRepository clientRepository;

    public List<Pedido> obtenerTodos() {
        return pedidoRepository.findAll();
    }

    public Pedido obtenerPorId(Integer id) {
        return pedidoRepository.findById(id).orElse(null);
    }

    public Pedido guardar(Pedido pedido) {
        return pedidoRepository.save(pedido);
    }

    public List<Pedido> obtenerPedidosPorFecha(List<Integer> dias, Integer anio, Integer mes_pedido) {
        return pedidoRepository.findByDiaInAndAnioAndMesPedidoOrderById(dias, anio, mes_pedido);
    }

    public List<String> getMesesPedido() {
        return pedidoRepository.getMesesPedido();
    }

    @Transactional
    public void reiniciarPedidos(List<Pedido> pedidos) {
        if (pedidos != null && !pedidos.isEmpty()) {
            List<Integer> ids = pedidos.stream()
                    .map(Pedido::getId)
                    .collect(Collectors.toList());
            pedidoRepository.reiniciarPedidosPorIds(ids);
        }
    }

    public List<Pedido> findByFechaPedidoBetween(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return pedidoRepository.findByfechaDeRegistroBetween(fechaInicio, fechaFin);
    }

    public void deleteById(Integer id) {
        pedidoRepository.deleteById(id);
    }

    public void deleteAll() {
        pedidoRepository.deleteAll();
    }

    public List<Pedido> cargarArchivo(MultipartFile file) {
        String filename = file.getOriginalFilename();
        assert filename != null;

        String anio = filename.substring(6, 10);
        String mes = filename.substring(10, 12);
        if (anio.isBlank() || mes.isBlank()) {
            throw new IllegalArgumentException("El nombre del archivo debe contener el año y mes en el formato correcto.");
        }

        List<Pedido> pedidos = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
            String registro;
            while ((registro = reader.readLine()) != null && !registro.isBlank()) {
                Pedido pedido = Pedido.leerRegistro(registro, Integer.parseInt(anio), Integer.parseInt(mes), 0);

                Optional<Cliente> clienteOptional = clientRepository.findById(pedido.getIdCliente());
                Cliente cliente;
                if (!clienteOptional.isPresent()) {
                    cliente = new Cliente();
                    cliente.setId(pedido.getIdCliente());
                    cliente.setNombre("Cliente " + pedido.getIdCliente());
                    cliente.setCorreo("cliente" + pedido.getIdCliente() + "@glplogistics.com.pe");
                    clientRepository.save(cliente);
                } else cliente = clienteOptional.get();

                pedido.setCliente(cliente);
                pedidos.add(pedidoRepository.save(pedido));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return pedidos;
    }

    public List<Pedido> getPedidosSemana(List<Pedido> pedidos, int dia, int mes, int anio, int hora, int minuto) {
        List<Pedido> pedidosSemana = new ArrayList<>();
        LocalDateTime fechaInicio = LocalDateTime.of(anio, mes, dia, hora, minuto);
        LocalDateTime fechaFin = fechaInicio.plusHours(168);

        for (Pedido pedido : pedidos) {
            LocalDateTime fechaPedido = pedido.getFechaDeRegistro();
            if (!fechaPedido.isBefore(fechaInicio) && !fechaPedido.isAfter(fechaFin)) {
                pedidosSemana.add(pedido);
            }
        }
        return pedidosSemana;
    }

    public List<Pedido> dividirPedidos(List<Pedido> pedidos, int tipoSimulacion) {
        if (tipoSimulacion == 1) {
            this.pedidosCarga = new ArrayList<>();
            for (Pedido pedido : pedidos) {
                int demanda = pedido.getCantidadGLP();
                while (demanda > 25) {
                    int cantidad = Math.min(demanda, 5);
                    Pedido pedido1 = new Pedido(pedido.getDia(), pedido.getHora(), pedido.getMinuto(),
                            pedido.getAnio(),
                            pedido.getMesPedido(), pedido.getPosX(), pedido.getPosY(), pedido.getIdCliente(), cantidad,
                            pedido.getHorasLimite(), pedido.isEntregado());
                    pedido1.setId(pedido.getId());
                    demanda = demanda - cantidad;
                    pedidosCarga.add(pedido1);
                }
                if (demanda <= 25) {
                    pedidosCarga.add(pedido);
                }
            }
            return pedidosCarga;
        } else {
            this.pedidosCarga = new ArrayList<>();
            for (Pedido pedido : pedidos) {
                int demanda = pedido.getCantidadGLP();
                while (demanda > 0) {
                    int cantidad = Math.min(demanda, 5); // Divide en pedidos de 5 o menos
                    Pedido pedido1 = new Pedido(pedido.getDia(), pedido.getHora(), pedido.getMinuto(),
                            pedido.getAnio(),
                            pedido.getMesPedido(), pedido.getPosX(), pedido.getPosY(), pedido.getIdCliente(), cantidad,
                            pedido.getHorasLimite(), pedido.isEntregado());
                    pedido1.setId(pedido.getId());
                    demanda = demanda - cantidad;
                    pedidosCarga.add(pedido1);
                }
            }

            return pedidosCarga;
        }

    }


}
