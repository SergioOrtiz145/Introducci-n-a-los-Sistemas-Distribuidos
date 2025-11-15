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
    private final String puertoReplicacionLocal;  // Puerto donde ESTE GA publica réplicas
    private final String direccionReplicaRemota;   // Dirección donde escucha el OTRO GA
    private final boolean esPrimario;
    private final String rutaBD;

    private ZContext context;
    private ZMQ.Socket socketServicio;
    private ZMQ.Socket socketHealthCheck;
    private ZMQ.Socket socketReplicacionPub;  // Para publicar réplicas
    private ZMQ.Socket socketReplicacionSub;  // Para recibir réplicas
    private final Gson gson;

    private volatile boolean activo = true;
    private volatile boolean bdDisponible = true;
    private final ScheduledExecutorService schedulerHealth;

    public GA(String sede, String rutaBD,
            String puertoServicio,
            String puertoHealthCheck,
            String puertoReplicacionLocal,
            String direccionReplicaRemota,
            boolean esPrimario) {
        this.sede = sede;
        this.rutaBD = rutaBD;
        this.puertoServicio = puertoServicio;
        this.puertoHealthCheck = puertoHealthCheck;
        this.puertoReplicacionLocal = puertoReplicacionLocal;
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

        // ==========================================
        // CAMBIO 1: Ambos GA publican réplicas
        // ==========================================
        socketReplicacionPub = context.createSocket(SocketType.PUB);
        socketReplicacionPub.bind("tcp://*:" + puertoReplicacionLocal);
        System.out.println("GA " + sede + " - Socket PUB replicación en puerto " + puertoReplicacionLocal);

        // ==========================================
        // CAMBIO 2: Ambos GA reciben réplicas
        // ==========================================
        if (direccionReplicaRemota != null && !direccionReplicaRemota.isEmpty()) {
            iniciarReceptorReplicas();
        }

        System.out.println("GA " + sede + " iniciado como " + (esPrimario ? "PRIMARIO" : "SECUNDARIO"));
        System.out.println("  → Publica réplicas en puerto: " + puertoReplicacionLocal);
        System.out.println("  → Escucha réplicas desde: " + direccionReplicaRemota);
    }

    private void iniciarMonitoreoSalud() {
        schedulerHealth.scheduleAtFixedRate(() -> {
            verificarSaludBD();
        }, 5, 5, TimeUnit.SECONDS);
    }

    private void verificarSaludBD() {
        try {
            boolean disponibleAhora = bdLocal.verificarDisponibilidad();

            if (!disponibleAhora && bdDisponible) {
                System.err.println("¡ALERTA! BD " + sede + " NO DISPONIBLE");
                bdDisponible = false;
                intentarRecuperacionBD();
            } else if (disponibleAhora && !bdDisponible) {
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
            socketReplicacionSub = context.createSocket(SocketType.SUB);
            socketReplicacionSub.connect(direccionReplicaRemota);
            socketReplicacionSub.subscribe("".getBytes());

            System.out.println("GA " + sede + " escuchando réplicas desde: " + direccionReplicaRemota);

            while (activo) {
                try {
                    String mensajeJson = socketReplicacionSub.recvStr();
                    
                    if (mensajeJson != null) {
                        Map<String, Object> operacion = gson.fromJson(mensajeJson,
                                new TypeToken<Map<String, Object>>() {}.getType());
                        
                        // ==========================================
                        // CAMBIO 3: Verificar que no sea réplica propia
                        // ==========================================
                        String sedeOrigen = (String) operacion.get("sedeOrigen");
                        if (!sede.equals(sedeOrigen)) {
                            System.out.println("📩 [" + sede + "] Réplica recibida desde " + sedeOrigen);
                            aplicarReplicacion(operacion);
                        }
                    }
                } catch (Exception e) {
                    if (activo) {
                        System.err.println("Error recibiendo réplica: " + e.getMessage());
                    }
                }
            }
            
            if (socketReplicacionSub != null) {
                socketReplicacionSub.close();
            }
        });
        hiloReceptor.setName("ReceptorReplicas-" + sede);
        hiloReceptor.start();
    }

    private void aplicarReplicacion(Map<String, Object> operacion) {
        String tipo = (String) operacion.get("operacion");
        System.out.println("  → Aplicando réplica: " + tipo);

        try {
            switch (tipo) {
                case "PRESTAMO":
                    String isbn = (String) operacion.get("isbn");
                    String usuario = (String) operacion.get("usuario");
                    String idPrestamo = (String) operacion.get("idPrestamo");
                    
                    System.out.println("    Datos: ISBN=" + isbn + ", Usuario=" + usuario + ", ID=" + idPrestamo);
                    
                    String resultado = bdLocal.realizarPrestamoReplica(isbn, usuario, idPrestamo);
                    
                    if (resultado != null) {
                        System.out.println("  ✓ Réplica de préstamo aplicada: " + resultado);
                    } else {
                        System.err.println("  ✗ ERROR: No se pudo aplicar réplica de préstamo");
                    }
                    break;
                    
                case "DEVOLUCION":
                    boolean exitoDevolucion = bdLocal.realizarDevolucion((String) operacion.get("idPrestamo"));
                    if (exitoDevolucion) {
                        System.out.println("  ✓ Réplica de devolución aplicada");
                    } else {
                        System.err.println("  ✗ ERROR: No se pudo aplicar réplica de devolución");
                    }
                    break;
                    
                case "RENOVACION":
                    boolean exitoRenovacion = bdLocal.realizarRenovacion((String) operacion.get("idPrestamo"));
                    if (exitoRenovacion) {
                        System.out.println("  ✓ Réplica de renovación aplicada");
                    } else {
                        System.err.println("  ✗ ERROR: No se pudo aplicar réplica de renovación");
                    }
                    break;
                    
                default:
                    System.err.println("  ✗ Tipo de réplica desconocido: " + tipo);
            }
        } catch (Exception e) {
            System.err.println("  ✗ Error aplicando réplica: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void ejecutar() {
        ZMQ.Poller poller = context.createPoller(2);
        poller.register(socketServicio, ZMQ.Poller.POLLIN);
        poller.register(socketHealthCheck, ZMQ.Poller.POLLIN);

        System.out.println("GA " + sede + " esperando solicitudes...\n");

        while (activo && !Thread.currentThread().isInterrupted()) {
            try {
                poller.poll(1000);

                if (poller.pollin(0)) {
                    procesarSolicitudServicio();
                }

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

            Map<String, Object> solicitud = gson.fromJson(mensajeJson, new TypeToken<Map<String, Object>>() {}.getType());
            Map<String, Object> respuesta = new HashMap<>();

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
        
        respuesta.put("exito", exitoPrestamo);
        respuesta.put("mensaje", exitoPrestamo ? "Préstamo realizado exitosamente" : "Libro no disponible");
        respuesta.put("operacion", "PRESTAMO");

        // ==========================================
        // CAMBIO 4: Ambos GA replican sus cambios
        // ==========================================
        if (exitoPrestamo) {
            // Crear objeto de réplica con el ID del préstamo
            Map<String, Object> datosReplicacion = new HashMap<>();
            datosReplicacion.put("operacion", "PRESTAMO");
            datosReplicacion.put("isbn", isbn);
            datosReplicacion.put("usuario", usuario);
            datosReplicacion.put("idPrestamo", idPrestamo);
            
            replicarOperacion(datosReplicacion);
        }
    }

    private void procesarDevolucion(Map<String, Object> solicitud, Map<String, Object> respuesta) {
        String idPrestamo = (String) solicitud.get("idPrestamo");

        boolean exitoDevolucion = bdLocal.realizarDevolucion(idPrestamo);
        respuesta.put("exito", exitoDevolucion);
        respuesta.put("mensaje", exitoDevolucion ? "Devolución registrada exitosamente" : "Préstamo no encontrado");
        respuesta.put("operacion", "DEVOLUCION");

        // ==========================================
        // CAMBIO 4: Ambos GA replican sus cambios
        // ==========================================
        if (exitoDevolucion) {
            Map<String, Object> datosReplicacion = new HashMap<>();
            datosReplicacion.put("operacion", "DEVOLUCION");
            datosReplicacion.put("idPrestamo", idPrestamo);
            
            replicarOperacion(datosReplicacion);
        }
    }

    private void procesarRenovacion(Map<String, Object> solicitud, Map<String, Object> respuesta) {
        String idPrestamo = (String) solicitud.get("idPrestamo");

        boolean exitoRenovacion = bdLocal.realizarRenovacion(idPrestamo);
        respuesta.put("exito", exitoRenovacion);
        respuesta.put("mensaje", exitoRenovacion ? "Renovación realizada exitosamente" : "No se puede renovar");
        respuesta.put("operacion", "RENOVACION");

        // ==========================================
        // CAMBIO 4: Ambos GA replican sus cambios
        // ==========================================
        if (exitoRenovacion) {
            Map<String, Object> datosReplicacion = new HashMap<>();
            datosReplicacion.put("operacion", "RENOVACION");
            datosReplicacion.put("idPrestamo", idPrestamo);
            
            replicarOperacion(datosReplicacion);
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
        if (socketReplicacionPub != null) {
            try {
                operacion.put("timestamp", System.currentTimeMillis());
                operacion.put("sedeOrigen", sede);
                String mensaje = gson.toJson(operacion);
                
                socketReplicacionPub.send(mensaje, ZMQ.DONTWAIT);
                System.out.println("📤 [" + sede + "] Réplica enviada: " + operacion.get("operacion"));
                
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
        if (args.length < 7) {
            System.out.println("Uso: java GA <sede> <rutaBD> <puertoServicio> " +
                    "<puertoHealthCheck> <puertoReplicacionLocal> <direccionReplicaRemota> <esPrimario>");
            System.out.println("\nEjemplo SEDE1:");
            System.out.println("  java GA SEDE1 ./datos/sede1 5555 5556 5655 tcp://localhost:6655 true");
            System.out.println("\nEjemplo SEDE2:");
            System.out.println("  java GA SEDE2 ./datos/sede2 6555 6556 6655 tcp://localhost:5655 false");
            return;
        }

        GA ga = new GA(
                args[0], // sede: "SEDE1" o "SEDE2"
                args[1], // rutaBD: "./datos/sede1"
                args[2], // puertoServicio: "5555"
                args[3], // puertoHealthCheck: "5556"
                args[4], // puertoReplicacionLocal: "5655" (donde ESTE GA publica)
                args[5], // direccionReplicaRemota: "tcp://localhost:6655" (de donde escucha)
                Boolean.parseBoolean(args[6]) // esPrimario: true/false
        );

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ga.cerrar();
        }));

        ga.ejecutar();
    }
}