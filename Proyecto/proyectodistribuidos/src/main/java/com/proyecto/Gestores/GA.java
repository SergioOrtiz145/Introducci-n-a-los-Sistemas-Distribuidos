package com.proyecto.Gestores;

import com.proyecto.Persistencia.BaseDatos;
import org.zeromq.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.*;
import java.util.concurrent.*;

public class GA {
    private BaseDatos bdLocal;
    private final String sede;
    private final String puertoServicio;
    private final String puertoHealthCheck;
    private final String direccionReplicaRemota;
    private final boolean esPrimario;
    private final String rutaBD;

    private ZContext context;
    private ZMQ.Socket socketServicio;
    private ZMQ.Socket socketHealthCheck;
    private ZMQ.Socket socketReplicacion;
    private final Gson gson;

    private volatile boolean activo = true;
    private volatile boolean bdDisponible = true;
    private final ScheduledExecutorService schedulerHealth;

    public GA(String sede, String rutaBD,
            String puertoServicio,
            String puertoHealthCheck,
            String direccionReplicaRemota,
            boolean esPrimario) {
        this.sede = sede;
        this.rutaBD = rutaBD;
        this.puertoServicio = puertoServicio;
        this.puertoHealthCheck = puertoHealthCheck;
        this.direccionReplicaRemota = direccionReplicaRemota;
        this.esPrimario = esPrimario;
        this.gson = new Gson();
        this.schedulerHealth = Executors.newScheduledThreadPool(1);

        inicializarBD();
        inicializarZeroMQ();
        iniciarMonitoreoSalud();
    }

    private void inicializarBD() {
        try {
            this.bdLocal = new BaseDatos(rutaBD, sede);
            this.bdDisponible = bdLocal.verificarDisponibilidad();
            System.out.println("BD " + sede + " inicializada. Disponible: " + bdDisponible);
        } catch (Exception e) {
            System.err.println("ERROR: Fallo al inicializar BD: " + e.getMessage());
            this.bdDisponible = false;
        }
    }

    private void inicializarZeroMQ() {
        context = new ZContext();

        // Socket REP para atender solicitudes de operaciones
        socketServicio = context.createSocket(SocketType.REP);
        socketServicio.bind("tcp://*:" + puertoServicio);
        System.out.println("GA " + sede + " - Socket servicio en puerto " + puertoServicio);

        // Socket REP para health checks
        socketHealthCheck = context.createSocket(SocketType.REP);
        socketHealthCheck.bind("tcp://*:" + puertoHealthCheck);
        System.out.println("GA " + sede + " - Socket health check en puerto " + puertoHealthCheck);

        // Socket PUB para replicación asíncrona (solo si es primario)
        if (esPrimario && direccionReplicaRemota != null) {
            socketReplicacion = context.createSocket(SocketType.PUB);
            int puertoReplicacion = Integer.parseInt(puertoServicio) + 100;
            socketReplicacion.bind("tcp://*:" + puertoReplicacion);
            System.out.println("GA " + sede + " - Socket replicación en puerto " + puertoReplicacion);
        }
        if (!esPrimario && direccionReplicaRemota != null) {
            // GA secundario recibe réplicas del primario
            iniciarReceptorReplicas();
        }
        System.out.println("GA " + sede + " iniciado como " + (esPrimario ? "PRIMARIO" : "SECUNDARIO"));
    }

    private void iniciarMonitoreoSalud() {
        // Verificar salud de la BD cada 5 segundos
        schedulerHealth.scheduleAtFixedRate(() -> {
            verificarSaludBD();
        }, 5, 5, TimeUnit.SECONDS);
    }

    private void verificarSaludBD() {
        try {
            boolean disponibleAhora = bdLocal.verificarDisponibilidad();

            if (!disponibleAhora && bdDisponible) {
                // La BD acaba de fallar
                System.err.println("¡ALERTA! BD " + sede + " NO DISPONIBLE");
                bdDisponible = false;
                intentarRecuperacionBD();
            } else if (disponibleAhora && !bdDisponible) {
                // La BD se recuperó
                System.out.println("BD " + sede + " RECUPERADA");
                bdDisponible = true;
            }
        } catch (Exception e) {
            System.err.println("Error verificando salud BD: " + e.getMessage());
            bdDisponible = false;
        }
    }

    private void intentarRecuperacionBD() {
        System.out.println("Intentando recuperar BD " + sede + "...");
        try {
            // Intentar reinicializar la BD
            Thread.sleep(2000);
            this.bdDisponible = bdLocal.verificarDisponibilidad();

            if (bdDisponible) {
                System.out.println("BD " + sede + " recuperada exitosamente");
            } else {
                System.err.println("No se pudo recuperar BD " + sede);
            }
        } catch (Exception e) {
            System.err.println("Error en recuperación BD: " + e.getMessage());
            this.bdDisponible = false;
        }
    }

    private void iniciarReceptorReplicas() {
        Thread hiloReceptor = new Thread(() -> {
            ZMQ.Socket socketSub = context.createSocket(SocketType.SUB);
            socketSub.connect(direccionReplicaRemota);
            socketSub.subscribe("".getBytes());

            System.out.println("GA " + sede + " escuchando réplicas de primario...");

            while (activo) {
                try {
                    String mensajeJson = socketSub.recvStr();
                    System.out.println(mensajeJson);
                    if (mensajeJson != null) {
                        Map<String, Object> operacion = gson.fromJson(mensajeJson,
                                new TypeToken<Map<String, Object>>() {
                                }.getType());
                        aplicarReplicacion(operacion);
                    }
                } catch (Exception e) {
                    System.err.println("Error recibiendo réplica: " + e.getMessage());
                }
            }
            socketSub.close();
        });
        hiloReceptor.start();
    }

    private void aplicarReplicacion(Map<String, Object> operacion) {
        String tipo = (String) operacion.get("operacion");
        System.out.println("Aplicando réplica: " + tipo);

        switch (tipo) {
            case "PRESTAMO":
                bdLocal.realizarPrestamoReplica(
                        (String) operacion.get("isbn"),
                        (String) operacion.get("usuario"),
                        (String) operacion.get("idPrestamo"));
                break;
            case "DEVOLUCION":
                bdLocal.realizarDevolucion((String) operacion.get("idPrestamo"));
                break;
            case "RENOVACION":
                bdLocal.realizarRenovacion((String) operacion.get("idPrestamo"));
                break;
        }
    }

    public void ejecutar() {
        // Poller para manejar múltiples sockets
        ZMQ.Poller poller = context.createPoller(2);
        poller.register(socketServicio, ZMQ.Poller.POLLIN);
        poller.register(socketHealthCheck, ZMQ.Poller.POLLIN);

        System.out.println("GA " + sede + " esperando solicitudes...\n");

        while (activo && !Thread.currentThread().isInterrupted()) {
            try {
                poller.poll(1000); // Timeout de 1 segundo

                // Procesar solicitudes de servicio
                if (poller.pollin(0)) {
                    procesarSolicitudServicio();
                }

                // Procesar health checks
                if (poller.pollin(1)) {
                    procesarHealthCheck();
                }

            } catch (Exception e) {
                System.err.println("Error en loop principal: " + e.getMessage());
            }
        }

        cerrar();
    }

    private void procesarSolicitudServicio() {
        try {
            String mensajeJson = socketServicio.recvStr();
            System.out.println(
                    "→ Solicitud recibida: " + mensajeJson.substring(0, Math.min(50, mensajeJson.length())) + "...");

            Map<String, Object> solicitud = gson.fromJson(mensajeJson, new TypeToken<Map<String, Object>>() {
            }.getType());
            Map<String, Object> respuesta = new HashMap<>();

            // Verificar si la BD está disponible
            if (!bdDisponible) {
                respuesta.put("exito", false);
                respuesta.put("mensaje", "BD no disponible. Usar réplica secundaria.");
                respuesta.put("error", "BD_NO_DISPONIBLE");
                socketServicio.send(gson.toJson(respuesta));
                System.err.println("Solicitud rechazada: BD no disponible");
                return;
            }

            String tipoOperacion = (String) solicitud.get("operacion");

            switch (tipoOperacion) {
                case "PRESTAMO":
                    procesarPrestamo(solicitud, respuesta);
                    break;

                case "DEVOLUCION":
                    procesarDevolucion(solicitud, respuesta);
                    break;

                case "RENOVACION":
                    procesarRenovacion(solicitud, respuesta);
                    break;

                default:
                    respuesta.put("exito", false);
                    respuesta.put("mensaje", "Operación desconocida: " + tipoOperacion);
            }

            socketServicio.send(gson.toJson(respuesta));
            System.out.println("← Respuesta enviada: " + respuesta.get("mensaje"));

        } catch (Exception e) {
            System.err.println("Error procesando solicitud: " + e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("exito", false);
            error.put("mensaje", "Error interno: " + e.getMessage());
            socketServicio.send(gson.toJson(error));
        }
    }

    private void procesarPrestamo(Map<String, Object> solicitud, Map<String, Object> respuesta) {
        String isbn = (String) solicitud.get("isbn");
        String usuario = (String) solicitud.get("usuario");

        String idPrestamo = bdLocal.realizarPrestamo(isbn, usuario);

        Boolean exitoPrestamo = idPrestamo != null;
        if (exitoPrestamo) {
            solicitud.put("idPrestamo", idPrestamo); 
        }
        respuesta.put("exito", exitoPrestamo);
        respuesta.put("mensaje", exitoPrestamo ? "Préstamo realizado exitosamente" : "Libro no disponible");
        respuesta.put("operacion", "PRESTAMO");

        if (exitoPrestamo && esPrimario) {
            replicarOperacion(solicitud);
        }
    }

    private void procesarDevolucion(Map<String, Object> solicitud, Map<String, Object> respuesta) {
        String idPrestamo = (String) solicitud.get("idPrestamo");

        boolean exitoDevolucion = bdLocal.realizarDevolucion(idPrestamo);
        respuesta.put("exito", exitoDevolucion);
        respuesta.put("mensaje", exitoDevolucion ? "Devolución registrada exitosamente" : "Préstamo no encontrado");
        respuesta.put("operacion", "DEVOLUCION");

        if (exitoDevolucion && esPrimario) {
            replicarOperacion(solicitud);
        }
    }

    private void procesarRenovacion(Map<String, Object> solicitud, Map<String, Object> respuesta) {
        String idPrestamo = (String) solicitud.get("idPrestamo");

        boolean exitoRenovacion = bdLocal.realizarRenovacion(idPrestamo);
        respuesta.put("exito", exitoRenovacion);
        respuesta.put("mensaje", exitoRenovacion ? "Renovación realizada exitosamente" : "No se puede renovar");
        respuesta.put("operacion", "RENOVACION");

        if (exitoRenovacion && esPrimario) {
            replicarOperacion(solicitud);
        }
    }

    private void procesarHealthCheck() {
        try {

            Map<String, Object> health = new HashMap<>();
            health.put("estado", activo ? "OK" : "FALLANDO");
            health.put("sede", sede);
            health.put("rol", esPrimario ? "PRIMARIO" : "SECUNDARIO");
            health.put("bdDisponible", bdDisponible);
            health.put("timestamp", System.currentTimeMillis());

            socketHealthCheck.send(gson.toJson(health));

        } catch (Exception e) {
            System.err.println("Error en health check: " + e.getMessage());
        }
    }

    private void replicarOperacion(Map<String, Object> operacion) {
        if (socketReplicacion != null) {
            try {
                operacion.put("timestamp", System.currentTimeMillis());
                operacion.put("sedeOrigen", sede);
                String mensaje = gson.toJson(operacion);
                System.out.println(mensaje);
                socketReplicacion.send(mensaje, ZMQ.DONTWAIT);
                System.out.println("Operación replicada a secundario");
            } catch (Exception e) {
                System.err.println("Error replicando operación: " + e.getMessage());
            }
        }
    }

    public void cerrar() {
        System.out.println("\nCerrando GA " + sede + "...");
        activo = false;
        schedulerHealth.shutdown();
        if (context != null) {
            context.close();
        }
        System.out.println("GA " + sede + " cerrado");
    }

    public static void main(String[] args) {
        if (args.length < 6) {
            System.out.println("Uso: java GA <sede> <rutaBD> " +
                    "<puertoServicio> <puertoHealthCheck> <direccionReplica> <esPrimario>");
            System.out.println("Ejemplo: java GA SEDE1 ./datos 5555 5556 tcp://192.168.1.101:5655 true");
            return;
        }

        GA ga = new GA(
                args[0], // sede: "SEDE1" o "SEDE2"
                args[1], // rutaBD: "./datos"
                args[2], // puertoServicio: "5555"
                args[3], // puertoHealthCheck: "5556"
                args[4], // direccionReplica: "tcp://192.168.1.101:5655"
                Boolean.parseBoolean(args[5]) // esPrimario: true/false
        );

        // Shutdown hook para cerrar limpiamente
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ga.cerrar();
        }));

        ga.ejecutar();
    }
}
