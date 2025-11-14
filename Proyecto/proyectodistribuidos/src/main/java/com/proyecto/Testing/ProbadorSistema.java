package com.proyecto.Testing;

import org.zeromq.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.*;
import java.io.*;
import java.nio.file.*;

/**
 * Clase para probar el sistema completo de préstamo de libros
 * Envía solicitudes a ambas sedes y verifica la replicación
 */
public class ProbadorSistema {
    private ZContext context;
    private Gson gson;
    private Map<String, ZMQ.Socket> sockets;
    private List<String> prestamosSede1;
    private List<String> prestamosSede2;

    public ProbadorSistema() {
        this.context = new ZContext();
        this.gson = new Gson();
        this.sockets = new HashMap<>();
        this.prestamosSede1 = new ArrayList<>();
        this.prestamosSede2 = new ArrayList<>();
    }

    private ZMQ.Socket conectar(String nombre, String direccion) {
        ZMQ.Socket socket = context.createSocket(SocketType.REQ);
        socket.connect(direccion);
        socket.setReceiveTimeOut(5000); // 5 segundos timeout
        sockets.put(nombre, socket);
        System.out.println("✓ Conectado a " + nombre + ": " + direccion);
        return socket;
    }

    private Map<String, Object> enviarSolicitud(String nombreSede, Map<String, Object> solicitud) {
        ZMQ.Socket socket = sockets.get(nombreSede);
        if (socket == null) {
            System.err.println("✗ Socket para " + nombreSede + " no existe");
            return null;
        }

        try {
            String solicitudJson = gson.toJson(solicitud);
            socket.send(solicitudJson);

            String respuestaJson = socket.recvStr();
            if (respuestaJson == null) {
                System.err.println("✗ Timeout esperando respuesta de " + nombreSede);
                return null;
            }

            return gson.fromJson(respuestaJson, new TypeToken<Map<String, Object>>(){}.getType());
        } catch (Exception e) {
            System.err.println("✗ Error comunicándose con " + nombreSede + ": " + e.getMessage());
            return null;
        }
    }

    public void probarPrestamo(String nombreSede, String isbn, String usuario) {
        System.out.println("\n→ [" + nombreSede + "] Solicitando préstamo: " + isbn + " para " + usuario);
        
        Map<String, Object> solicitud = new HashMap<>();
        solicitud.put("operacion", "PRESTAMO");
        solicitud.put("isbn", isbn);
        solicitud.put("usuario", usuario);

        Map<String, Object> respuesta = enviarSolicitud(nombreSede, solicitud);
        
        if (respuesta != null) {
            boolean exito = (boolean) respuesta.get("exito");
            String mensaje = (String) respuesta.get("mensaje");
            
            if (exito) {
                System.out.println("✓ Préstamo exitoso: " + mensaje);
                // Guardar para usar en devoluciones/renovaciones
                if (nombreSede.equals("SEDE1")) {
                    prestamosSede1.add(isbn + ":" + usuario);
                } else {
                    prestamosSede2.add(isbn + ":" + usuario);
                }
            } else {
                System.out.println("✗ Préstamo fallido: " + mensaje);
            }
        }
    }

    public void probarDevolucion(String nombreSede, String idPrestamo) {
        System.out.println("\n→ [" + nombreSede + "] Solicitando devolución: " + idPrestamo);
        
        Map<String, Object> solicitud = new HashMap<>();
        solicitud.put("operacion", "DEVOLUCION");
        solicitud.put("idPrestamo", idPrestamo);

        Map<String, Object> respuesta = enviarSolicitud(nombreSede, solicitud);
        
        if (respuesta != null) {
            boolean exito = (boolean) respuesta.get("exito");
            String mensaje = (String) respuesta.get("mensaje");
            System.out.println(exito ? "✓ " + mensaje : "✗ " + mensaje);
        }
    }

    public void probarRenovacion(String nombreSede, String idPrestamo) {
        System.out.println("\n→ [" + nombreSede + "] Solicitando renovación: " + idPrestamo);
        
        Map<String, Object> solicitud = new HashMap<>();
        solicitud.put("operacion", "RENOVACION");
        solicitud.put("idPrestamo", idPrestamo);

        Map<String, Object> respuesta = enviarSolicitud(nombreSede, solicitud);
        
        if (respuesta != null) {
            boolean exito = (boolean) respuesta.get("exito");
            String mensaje = (String) respuesta.get("mensaje");
            System.out.println(exito ? "✓ " + mensaje : "✗ " + mensaje);
        }
    }

    public void verificarEstadoBD(String rutaBD, String nombreSede) {
        System.out.println("\n========================================");
        System.out.println("  ESTADO BD " + nombreSede);
        System.out.println("========================================");

        try {
            // Leer libros
            String rutaLibros = rutaBD + "/libros_" + nombreSede + ".txt";
            List<String> libros = Files.readAllLines(Paths.get(rutaLibros));
            System.out.println("\n📚 Libros (primeros 5):");
            for (int i = 0; i < Math.min(5, libros.size()); i++) {
                System.out.println("  " + libros.get(i));
            }
            System.out.println("  Total libros: " + libros.size());

            // Leer préstamos
            String rutaPrestamos = rutaBD + "/prestamos_" + nombreSede + ".txt";
            List<String> prestamos = Files.readAllLines(Paths.get(rutaPrestamos));
            System.out.println("\n📋 Préstamos activos:");
            if (prestamos.isEmpty()) {
                System.out.println("  (No hay préstamos activos)");
            } else {
                for (String prestamo : prestamos) {
                    System.out.println("  " + prestamo);
                }
            }
            System.out.println("  Total préstamos: " + prestamos.size());

        } catch (IOException e) {
            System.err.println("✗ Error leyendo BD: " + e.getMessage());
        }
        
        System.out.println("========================================\n");
    }

    public void ejecutarBateriaPruebas() {
        System.out.println("\n");
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║   BATERÍA DE PRUEBAS DEL SISTEMA      ║");
        System.out.println("╔════════════════════════════════════════╗");

        // Conectar a ambas sedes
        conectar("SEDE1", "tcp://localhost:5555");
        conectar("SEDE2", "tcp://localhost:6555");

        System.out.println("\n═══ PRUEBA 1: Préstamos en SEDE1 ═══");
        probarPrestamo("SEDE1", "ISBN0001", "estudiante001");
        probarPrestamo("SEDE1", "ISBN0002", "profesor001");
        probarPrestamo("SEDE1", "ISBN0003", "estudiante002");
        
        System.out.println("\n⏱️  Esperando 2 segundos para replicación...");
        esperar(2000);

        System.out.println("\n═══ PRUEBA 2: Préstamos en SEDE2 ═══");
        probarPrestamo("SEDE2", "ISBN0010", "estudiante003");
        probarPrestamo("SEDE2", "ISBN0011", "profesor002");
        
        System.out.println("\n⏱️  Esperando 2 segundos para replicación...");
        esperar(2000);

        System.out.println("\n═══ PRUEBA 3: Renovaciones ═══");
        // Usar IDs de préstamos existentes (generados al inicio)
        probarRenovacion("SEDE1", "PREST-SEDE1-0001");
        probarRenovacion("SEDE2", "PREST-SEDE2-0001");
        
        System.out.println("\n⏱️  Esperando 2 segundos para replicación...");
        esperar(2000);

        System.out.println("\n═══ PRUEBA 4: Devoluciones ═══");
        probarDevolucion("SEDE1", "PREST-SEDE1-0002");
        probarDevolucion("SEDE2", "PREST-SEDE2-0002");
        
        System.out.println("\n⏱️  Esperando 2 segundos para replicación...");
        esperar(2000);

        System.out.println("\n═══ PRUEBA 5: Préstamo de libro no disponible ═══");
        probarPrestamo("SEDE1", "ISBN9999", "estudiante999");

        System.out.println("\n═══ PRUEBA 6: Múltiples préstamos del mismo libro ═══");
        probarPrestamo("SEDE1", "ISBN0001", "estudiante004");
        probarPrestamo("SEDE1", "ISBN0001", "estudiante005");
        probarPrestamo("SEDE1", "ISBN0001", "estudiante006");
        
        System.out.println("\n⏱️  Esperando 3 segundos para que se apliquen todas las réplicas...");
        esperar(3000);

        // Verificar estado de las BDs
        verificarEstadoBD("./datos/sede1", "SEDE1");
        verificarEstadoBD("./datos/sede2", "SEDE2");

        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║      PRUEBAS COMPLETADAS              ║");
        System.out.println("╚════════════════════════════════════════╝\n");
    }

    public void ejecutarPruebasCarga(int numOperaciones) {
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║      PRUEBA DE CARGA                  ║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println("Operaciones totales: " + numOperaciones);

        conectar("SEDE1", "tcp://localhost:5555");
        conectar("SEDE2", "tcp://localhost:6555");

        Random random = new Random();
        long tiempoInicio = System.currentTimeMillis();
        int exitosas = 0;
        int fallidas = 0;

        for (int i = 0; i < numOperaciones; i++) {
            String sede = random.nextBoolean() ? "SEDE1" : "SEDE2";
            String isbn = String.format("ISBN%04d", random.nextInt(1000) + 1);
            String usuario = "user" + random.nextInt(100);
            
            int tipoOp = random.nextInt(3);
            
            Map<String, Object> respuesta = null;
            switch (tipoOp) {
                case 0: // Préstamo
                    Map<String, Object> solicitudPrestamo = new HashMap<>();
                    solicitudPrestamo.put("operacion", "PRESTAMO");
                    solicitudPrestamo.put("isbn", isbn);
                    solicitudPrestamo.put("usuario", usuario);
                    respuesta = enviarSolicitud(sede, solicitudPrestamo);
                    break;
                case 1: // Renovación
                    Map<String, Object> solicitudRenovacion = new HashMap<>();
                    solicitudRenovacion.put("operacion", "RENOVACION");
                    solicitudRenovacion.put("idPrestamo", "PREST-" + sede + "-0001");
                    respuesta = enviarSolicitud(sede, solicitudRenovacion);
                    break;
                case 2: // Devolución
                    Map<String, Object> solicitudDevolucion = new HashMap<>();
                    solicitudDevolucion.put("operacion", "DEVOLUCION");
                    solicitudDevolucion.put("idPrestamo", "PREST-" + sede + "-0001");
                    respuesta = enviarSolicitud(sede, solicitudDevolucion);
                    break;
            }

            if (respuesta != null && (boolean) respuesta.get("exito")) {
                exitosas++;
            } else {
                fallidas++;
            }

            if ((i + 1) % 10 == 0) {
                System.out.print(".");
            }
        }

        long tiempoTotal = System.currentTimeMillis() - tiempoInicio;

        System.out.println("\n\n========================================");
        System.out.println("  RESULTADOS PRUEBA DE CARGA");
        System.out.println("========================================");
        System.out.println("Total operaciones: " + numOperaciones);
        System.out.println("Exitosas: " + exitosas);
        System.out.println("Fallidas: " + fallidas);
        System.out.println("Tiempo total: " + tiempoTotal + " ms");
        System.out.println("Promedio: " + (tiempoTotal / (double) numOperaciones) + " ms/op");
        System.out.println("Throughput: " + (numOperaciones * 1000.0 / tiempoTotal) + " op/s");
        System.out.println("========================================\n");
    }

    private void esperar(int milisegundos) {
        try {
            Thread.sleep(milisegundos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void cerrar() {
        for (ZMQ.Socket socket : sockets.values()) {
            socket.close();
        }
        context.close();
        System.out.println("✓ Conexiones cerradas");
    }

    public static void main(String[] args) {
        ProbadorSistema probador = new ProbadorSistema();

        try {
            if (args.length > 0 && args[0].equals("carga")) {
                int numOps = args.length > 1 ? Integer.parseInt(args[1]) : 100;
                probador.ejecutarPruebasCarga(numOps);
            } else {
                probador.ejecutarBateriaPruebas();
            }
        } catch (Exception e) {
            System.err.println("Error en pruebas: " + e.getMessage());
            e.printStackTrace();
        } finally {
            probador.cerrar();
        }
    }
}