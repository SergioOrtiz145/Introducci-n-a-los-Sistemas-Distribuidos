/**
 * ============================================================
 * Título: ClientePS
 * Autores: Ana Sofia Grass, Sergio Ortiz, Isabella Palacio, Sebastián Vargas
 * Fecha: 2025-11-15
 * ============================================================
 * ClientePS lee solicitudes de un archivo y las envía al GC.
 * Maneja préstamos, devoluciones y renovaciones con validación.
 */
package com.proyecto;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.zeromq.SocketType;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;

public class ClientePS {
    
    private final String nombreCliente;
    private final String archivoSolicitudes;
    private final String direccionGC;
    private final Map<String, String> prestamosActivos; // ISBN -> ID_PRESTAMO
    
    public ClientePS(String nombreCliente, String archivoSolicitudes, String direccionGC) {
        this.nombreCliente = nombreCliente;
        this.archivoSolicitudes = archivoSolicitudes;
        this.direccionGC = direccionGC;
        this.prestamosActivos = new HashMap<>();
    }
    
    /**
     * Lee el archivo de solicitudes y valida formato
     */
    public List<String> leerSolicitudes() {
        List<String> solicitudes = new ArrayList<>();
        
        try {
            if (!Files.exists(Paths.get(archivoSolicitudes))) {
                System.err.println(" El archivo no existe: " + archivoSolicitudes);
                return solicitudes;            }
            
            List<String> lineas = Files.readAllLines(Paths.get(archivoSolicitudes));
            int numeroLinea = 0;
            
            for (String linea : lineas) {
                numeroLinea++;
                linea = linea.trim();
                
                // Ignorar líneas vacías y comentarios
                if (linea.isEmpty() || linea.startsWith("#")) {
                    continue;
                }
                
                // Validar formato básico
                if (validarFormatoSolicitud(linea, numeroLinea)) {
                    solicitudes.add(linea);
                }
            }
            
            System.out.println("\n Archivo leido correctamente");
            System.out.println("Total de lineas: " + numeroLinea);
            System.out.println("Solicitudes válidas: " + solicitudes.size());
            
            if (solicitudes.size() < 20) {
                System.err.println("\nEl archivo tiene menos de 20 solicitudes");
                System.err.println("Se requieren al menos 20 para cumplir requisitos del proyecto");
            }
            
        } catch (IOException e) {
            System.err.println(" Error leyendo archivo: " + e.getMessage());
        }        
        return solicitudes;
    }
    
    /**
     * Valida el formato de una solicitud
     */
    private boolean validarFormatoSolicitud(String linea, int numeroLinea) {
        String[] partes = linea.split(",");
        
        if (partes.length < 2) {
            System.err.println("   Linea " + numeroLinea + ": Formato invalido (mínimo 2 campos)");
            return false;        }
        
        String operacion = partes[0].trim().toUpperCase();
        
        switch (operacion) {
            case "PRESTAR":
                if (partes.length < 2) {
                    System.err.println("Linea " + numeroLinea + ": PRESTAR requiere ISBN");
                    return false;                }
                break;
                
            case "DEVOLVER":
            case "RENOVAR":
                if (partes.length < 2) {
                    System.err.println("Linea " + numeroLinea + ": " + operacion + 
                                     " requiere ISBN o ID_PRESTAMO");
                    return false;
                }
                break;
                
            default:
                System.err.println("Linea " + numeroLinea + ": Operación desconocida: " + operacion);
                return false;        }
        
        return true;
    }
    
    /**
     * Procesa una solicitud individual
     */
    private void procesarSolicitud(Socket socket, String solicitudOriginal, int numeroSolicitud) {
        String[] partes = solicitudOriginal.split(",");
        String operacion = partes[0].trim().toUpperCase();
        String parametro = partes.length > 1 ? partes[1].trim() : "";
        String usuario;
        
        String solicitudFinal = "";
        String descripcion = "";
        
        switch (operacion) {
            case "PRESTAR":
                // Formato: PRESTAR,ISBN,USUARIO
                usuario = partes.length > 2 ? partes[2].trim() : nombreCliente;
                solicitudFinal = String.format("PRESTAR,%s,%s", parametro, usuario);
                descripcion = String.format("Prestamo de %s para %s", parametro, usuario);
                break;
                
            case "DEVOLVER":
                // Formato: DEVOLVER,ISBN,USUARIO
                // Si es ISBN, buscar el ID del préstamo activo
                usuario = partes.length > 2 ? partes[2].trim() : nombreCliente;
                solicitudFinal = String.format("DEVOLVER,%s,%s", parametro, usuario);
                descripcion = String.format("Devolucion de prestamo %s", parametro);
                break;
                
            case "RENOVAR":
                // Formato: RENOVAR,ISBN,USUARIO
                usuario = partes.length > 2 ? partes[2].trim() : nombreCliente;
                solicitudFinal = String.format("RENOVAR,%s,%s", parametro, usuario);
                descripcion = String.format("Renovacion de prestamo %s", parametro);
                break;
        }
        
        // Enviar solicitud
        System.out.println(String.format("\n[%d/%d] ================================================", 
                                        numeroSolicitud, -1));
        System.out.println("Enviando: " + descripcion);
        System.out.println("Solicitud: " + solicitudFinal);
        
        socket.send(solicitudFinal.getBytes(ZMQ.CHARSET), 0);
        
        // Recibir respuesta
        String respuesta = socket.recvStr(0);
        
        if (respuesta != null) {
            System.out.println("Respuesta: " + respuesta);
            
            // Actualizar préstamos activos según la respuesta
            actualizarEstadoPrestamos(operacion, parametro, respuesta);
            
            // Mostrar estado de préstamos activos
            if (!prestamosActivos.isEmpty()) {
                System.out.println("Prestamos activos: " + prestamosActivos.size());
            }
        } else {
            System.err.println("   Sin respuesta del servidor (timeout)");
        }        
        // Pequeña pausa entre solicitudes
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Actualiza el registro de préstamos activos según la respuesta
     */
    private void actualizarEstadoPrestamos(String operacion, String parametro, String respuesta) {
        if (operacion.equals("PRESTAR") && respuesta.contains("EXITOSO")) {
            // Extraer ID del préstamo si está en la respuesta
            // Por simplicidad, usar el ISBN como clave temporal
            // En producción, parsear el ID real de la respuesta
            prestamosActivos.put(parametro, "PREST-" + parametro);
        } else if (operacion.equals("DEVOLVER") && respuesta.contains("ACEPTADA")) {
            // Remover de préstamos activos
            prestamosActivos.values().remove(parametro);
        }
    }
    
    /**
     * Ejecuta todas las solicitudes del archivo
     */
    public void ejecutarSolicitudes() {
        List<String> solicitudes = leerSolicitudes();
        
        if (solicitudes.isEmpty()) {
            System.err.println("\n No hay solicitudes válidas para procesar");
            return;        }
        
        System.out.println("\n=====================================================");
        System.out.println("  CLIENTE PS: " + nombreCliente);
        System.out.println("  Conectando a: " + direccionGC);
        System.out.println("=====================================================");
        
        try (ZContext context = new ZContext()) {
            Socket socket = context.createSocket(SocketType.REQ);
            socket.connect(direccionGC);
            socket.setReceiveTimeOut(10000); // 10 segundos timeout
            
            System.out.println("\n Conectado al Gestor de Carga");
            System.out.println("\nIniciando procesamiento de solicitudes...");
            
            int numeroSolicitud = 1;
            int exitosas = 0;
            int fallidas = 0;
            
            for (String solicitud : solicitudes) {
                try {
                    procesarSolicitud(socket, solicitud, numeroSolicitud);
                    exitosas++;
                } catch (Exception e) {
                    System.err.println("   Error procesando solicitud: " + e.getMessage());
                    fallidas++;                }
                numeroSolicitud++;
            }
            
            // Resumen final
            System.out.println("\n=====================================================");
            System.out.println("  RESUMEN DE EJECUCIÓN");
            System.out.println("=====================================================");
            System.out.println("  Total procesadas: " + solicitudes.size());
            System.out.println("   Exitosas: " + exitosas);
            System.out.println("   Fallidas: " + fallidas);
            System.out.println("   Préstamos activos al final: " + prestamosActivos.size());
            System.out.println("=====================================================\n");
            
        } catch (Exception e) {
            System.err.println("\n Error de conexión: " + e.getMessage());
            e.printStackTrace();        }
    }
    
    /**
     * Método main
     */
    public static void main(String[] args) {
        // Configuración por defecto o desde argumentos
        String nombreCliente = args.length > 0 ? args[0] : "cliente_default";
        String archivoSolicitudes = args.length > 1 ? args[1] : "solicitudes.txt";
        String direccionGC = args.length > 2 ? args[2] : "tcp://localhost:5565";
        
        System.out.println("CLIENTE PS - PROCESO SOLICITANTE");
        System.out.println("\nConfiguración:");
        System.out.println("  • Cliente: " + nombreCliente);
        System.out.println("  • Archivo: " + archivoSolicitudes);
        System.out.println("  • GC: " + direccionGC);
        
        ClientePS cliente = new ClientePS(nombreCliente, archivoSolicitudes, direccionGC);
        cliente.ejecutarSolicitudes();
    }
}