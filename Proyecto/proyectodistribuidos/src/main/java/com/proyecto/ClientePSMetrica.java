/**
 * ============================================================
 * Título: ClientePSMetrica
 * Autores: Ana Sofia Grass, Sergio Ortiz, Isabella Palacio, Sebastián Vargas
 * Fecha: 2025-11-15
 * ============================================================
 * ClientePSMetrica lee solicitudes de un archivo y las envía al GC.
 * Maneja préstamos, devoluciones y renovaciones con validación.
 * Además realiza la medición del tiempo de respuesta promedio, su desviación estándar y cálculo de throughput, de cada cliente que se ejecuta. 
 */

package com.proyecto;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.zeromq.SocketType;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;

public class ClientePSMetrica {
    
    private final String nombreCliente;
    private final String archivoSolicitudes;
    private final String direccionGC;
    
    // Métricas
    private List<Long> tiemposRespuesta = new ArrayList<>();
    private long tiempoInicio;
    private long tiempoFin;
    private int totalProcesadas = 0;
    private int totalExitosas = 0;
    private int totalFallidas = 0;
    
    public ClientePSMetrica(String nombreCliente, String archivoSolicitudes, String direccionGC) {
        this.nombreCliente = nombreCliente;
        this.archivoSolicitudes = archivoSolicitudes;
        this.direccionGC = direccionGC;
    }
    
    public List<String> leerSolicitudes() {
        List<String> solicitudes = new ArrayList<>();
        
        try {
            if (!Files.exists(Paths.get(archivoSolicitudes))) {
                System.err.println("ERROR: El archivo no existe: " + archivoSolicitudes);
                return solicitudes;
            }
            
            List<String> lineas = Files.readAllLines(Paths.get(archivoSolicitudes));
            
            for (String linea : lineas) {
                linea = linea.trim();
                if (linea.isEmpty() || linea.startsWith("#")) continue;
                
                if (validarFormatoSolicitud(linea)) {
                    solicitudes.add(linea);
                }
            }
            
            System.out.println("Solicitudes válidas cargadas: " + solicitudes.size());
            
        } catch (IOException e) {
            System.err.println("Error leyendo archivo: " + e.getMessage());
        }
        
        return solicitudes;
    }
    
    private boolean validarFormatoSolicitud(String linea) {
        String[] partes = linea.split(",");
        if (partes.length < 2) return false;
        
        String operacion = partes[0].trim().toUpperCase();
        return operacion.equals("PRESTAR") || 
               operacion.equals("DEVOLVER") || 
               operacion.equals("RENOVAR");
    }
    
    private void procesarSolicitud(Socket socket, String solicitudOriginal, int numeroSolicitud) {
        String[] partes = solicitudOriginal.split(",");
        String operacion = partes[0].trim().toUpperCase();
        String parametro = partes.length > 1 ? partes[1].trim() : "";
        String usuario = partes.length > 2 ? partes[2].trim() : nombreCliente;
        
        String solicitudFinal = String.format("%s,%s,%s", operacion, parametro, usuario);
        
        // MEDIR TIEMPO DE INICIO
        long tiempoInicioOp = System.nanoTime();
        
        socket.send(solicitudFinal.getBytes(ZMQ.CHARSET), 0);
        String respuesta = socket.recvStr(0);
        
        // MEDIR TIEMPO DE FIN
        long tiempoFinOp = System.nanoTime();
        long tiempoRespuesta = (tiempoFinOp - tiempoInicioOp) / 1_000_000; // Convertir a ms
        
        if (respuesta != null) {
            totalProcesadas++;
            
            // Guardar tiempo según tipo de operación
            tiemposRespuesta.add(tiempoRespuesta);
            
            if (respuesta.contains("EXITOSO") || respuesta.contains("ACEPTADA")) {
                totalExitosas++;
            } else {
                totalFallidas++;
            }
        } else {
            totalFallidas++;
        }
    }
    
    public void ejecutarSolicitudes() {
        List<String> solicitudes = leerSolicitudes();
        
        if (solicitudes.isEmpty()) {
            System.err.println("No hay solicitudes para procesar");
            return;
        }
        
        System.out.println("\n=====================================================");
        System.out.println("  CLIENTE PS: " + nombreCliente);
        System.out.println("  Conectando a: " + direccionGC);
        System.out.println("=====================================================");
        
        try (ZContext context = new ZContext()) {
            Socket socket = context.createSocket(SocketType.REQ);
            socket.connect(direccionGC);
            socket.setReceiveTimeOut(10000);
            
            System.out.println("Conectado al Gestor de Carga");
            System.out.println("\nIniciando procesamiento...\n");
            
            // INICIAR TEMPORIZADOR
            tiempoInicio = System.currentTimeMillis();
            
            int numeroSolicitud = 1;
            for (String solicitud : solicitudes) {
                try {
                    procesarSolicitud(socket, solicitud, numeroSolicitud);
                } catch (Exception e) {
                    System.err.println("Error procesando solicitud: " + e.getMessage());
                    totalFallidas++;
                }
                numeroSolicitud++;
            }
            
            // FINALIZAR TEMPORIZADOR
            tiempoFin = System.currentTimeMillis();
            
            // MOSTRAR MÉTRICAS
            mostrarMetricas();
            
        } catch (Exception e) {
            System.err.println("Error de conexión: " + e.getMessage());
        }
    }
    
    private void mostrarMetricas() {
        long duracionTotal = tiempoFin - tiempoInicio;
        double duracionSegundos = duracionTotal / 1000.0;
        
        System.out.println("\n=====================================================");
        System.out.println("  MÉTRICAS DE RENDIMIENTO - " + nombreCliente);
        System.out.println("=====================================================");
        
        System.out.println("\n--- MÉTRICAS GENERALES ---");
        System.out.println("Total procesadas: " + totalProcesadas);
        System.out.println("Exitosas: " + totalExitosas);
        System.out.println("Fallidas: " + totalFallidas);
        System.out.printf("Duración total: %.3f segundos%n", duracionSegundos);
        
        if (!tiemposRespuesta.isEmpty()) {
            double promedio = calcularPromedio(tiemposRespuesta);
            double desviacion = calcularDesviacionEstandar(tiemposRespuesta, promedio);

            System.out.println("\n--- TIEMPO DE RESPUESTA GENERAL ---");
            System.out.println("Total solicitudes medidas: " + tiemposRespuesta.size());
            System.out.printf("Tiempo promedio: %.2f ms%n", promedio);
            System.out.printf("Desviación estándar: %.2f ms%n", desviacion);
        }
        
        // THROUGHPUT
        double throughput = totalProcesadas / duracionSegundos;
        double solicitudesEn2Min = throughput * 120;
        
        System.out.println("\n--- THROUGHPUT ---");
        System.out.printf("Solicitudes/segundo: %.2f%n", throughput);
        System.out.printf("Solicitudes procesadas en 2 min: %.0f%n", solicitudesEn2Min);
        
        System.out.println("=====================================================\n");
    }
    
    private double calcularPromedio(List<Long> tiempos) {
        if (tiempos.isEmpty()) return 0.0;
        
        long suma = 0;
        for (Long tiempo : tiempos) {
            suma += tiempo;
        }
        return (double) suma / tiempos.size();
    }
    
    private double calcularDesviacionEstandar(List<Long> tiempos, double promedio) {
        if (tiempos.size() < 2) return 0.0;
        
        double sumaCuadrados = 0;
        for (Long tiempo : tiempos) {
            double diferencia = tiempo - promedio;
            sumaCuadrados += diferencia * diferencia;
        }
        
        return Math.sqrt(sumaCuadrados / (tiempos.size() - 1));
    }

    public static void main(String[] args) {

    String archivoSolicitudes = args.length > 0 ? args[0] : "solicitudes.txt";
    String direccionGC = args.length > 1 ? args[1] : "tcp://localhost:5565";

    int numeroClientes = 5;

    System.out.println("CLIENTES PS SIMULTÁNEOS CON MÉTRICAS");
    System.out.println("Archivo: " + archivoSolicitudes);
    System.out.println("GC: " + direccionGC);
    System.out.println("Cantidad de clientes: " + numeroClientes);
    System.out.println("====================================\n");

    List<Thread> hilos = new ArrayList<>();

    for (int i = 1; i <= numeroClientes; i++) {

        String nombreCliente = "Cliente_" + i;

        Thread hilo = new Thread(() -> {
            System.out.println("\n>>> INICIANDO " + nombreCliente);

            ClientePSMetrica cliente = new ClientePSMetrica(
                    nombreCliente,
                    archivoSolicitudes,
                    direccionGC
            );

            cliente.ejecutarSolicitudes();

            System.out.println("\n<<< FINALIZÓ " + nombreCliente);
        });

        hilos.add(hilo);
        hilo.start();  
    }

    // Esperar a que todos terminen
    for (Thread hilo : hilos) {
        try {
            hilo.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    System.out.println("\nTODOS LOS CLIENTES SIMULTÁNEOS TERMINARON");
}
}
