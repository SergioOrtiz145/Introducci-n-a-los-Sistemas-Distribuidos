package com.proyecto.Testing;



import org.zeromq.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Verifica que la replicación entre sedes esté funcionando correctamente
 */
public class VerificadorReplicacion {
    private ZContext context;
    private Gson gson;

    public VerificadorReplicacion() {
        this.context = new ZContext();
        this.gson = new Gson();
    }

    private int contarLineas(String rutaArchivo) {
        try {
            return (int) Files.lines(Paths.get(rutaArchivo))
                    .filter(line -> !line.trim().isEmpty())
                    .count();
        } catch (IOException e) {
            return 0;
        }
    }

    private List<String> leerUltimasLineas(String rutaArchivo, int cantidad) {
        try {
            List<String> todasLineas = Files.readAllLines(Paths.get(rutaArchivo));
            int inicio = Math.max(0, todasLineas.size() - cantidad);
            return todasLineas.subList(inicio, todasLineas.size());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    private Map<String, Integer> obtenerEstadoBD(String sede) {
        Map<String, Integer> estado = new HashMap<>();
        String rutaBase = "./datos/" + sede.toLowerCase();
        
        estado.put("libros", contarLineas(rutaBase + "/libros_" + sede + ".txt"));
        estado.put("prestamos", contarLineas(rutaBase + "/prestamos_" + sede + ".txt"));
        
        return estado;
    }

    private void mostrarEstado(String titulo) {
        System.out.println("\n" + "=");
        System.out.println("  " + titulo);
        System.out.println("=");
        
        Map<String, Integer> estadoSede1 = obtenerEstadoBD("SEDE1");
        Map<String, Integer> estadoSede2 = obtenerEstadoBD("SEDE2");
        
        System.out.println("\n┌─────────────────┬──────────┬──────────┬──────────┐");
        System.out.println("│     Sede        │  Libros  │ Préstamos│  Estado  │");
        System.out.println("├─────────────────┼──────────┼──────────┼──────────┤");
        System.out.printf("│ SEDE1           │  %6d  │  %6d  │    %s   │%n", 
            estadoSede1.get("libros"), 
            estadoSede1.get("prestamos"),
            "✓");
        System.out.printf("│ SEDE2           │  %6d  │  %6d  │    %s   │%n", 
            estadoSede2.get("libros"), 
            estadoSede2.get("prestamos"),
            "✓");
        System.out.println("└─────────────────┴──────────┴──────────┴──────────┘");
        
        // Mostrar últimos préstamos
        System.out.println("\n📋 Últimos 3 préstamos SEDE1:");
        List<String> ultimosSede1 = leerUltimasLineas("./datos/sede1/prestamos_SEDE1.txt", 3);
        if (ultimosSede1.isEmpty()) {
            System.out.println("  (Sin préstamos)");
        } else {
            for (String linea : ultimosSede1) {
                System.out.println("  " + linea);
            }
        }
        
        System.out.println("\n📋 Últimos 3 préstamos SEDE2:");
        List<String> ultimosSede2 = leerUltimasLineas("./datos/sede2/prestamos_SEDE2.txt", 3);
        if (ultimosSede2.isEmpty()) {
            System.out.println("  (Sin préstamos)");
        } else {
            for (String linea : ultimosSede2) {
                System.out.println("  " + linea);
            }
        }
    }

    private Map<String, Object> enviarOperacion(String direccion, Map<String, Object> solicitud) {
        ZMQ.Socket socket = context.createSocket(SocketType.REQ);
        socket.connect(direccion);
        socket.setReceiveTimeOut(5000);
        
        try {
            socket.send(gson.toJson(solicitud));
            String respuesta = socket.recvStr();
            if (respuesta != null) {
                return gson.fromJson(respuesta, new TypeToken<Map<String, Object>>(){}.getType());
            }
        } finally {
            socket.close();
        }
        return null;
    }

    public void pruebaReplicacion() {
        System.out.println("\n");
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║     VERIFICADOR DE REPLICACIÓN                      ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        
        // Estado inicial
        mostrarEstado("ESTADO INICIAL");
        
        Map<String, Integer> inicialSede1 = obtenerEstadoBD("SEDE1");
        Map<String, Integer> inicialSede2 = obtenerEstadoBD("SEDE2");
        
        System.out.println("\n⏳ Enviando operación a SEDE1...");
        
        // Enviar préstamo a SEDE1
        Map<String, Object> solicitud = new HashMap<>();
        solicitud.put("operacion", "PRESTAMO");
        solicitud.put("isbn", "ISBN0001");
        solicitud.put("usuario", "testuser_" + System.currentTimeMillis());
        
        Map<String, Object> respuesta = enviarOperacion("tcp://localhost:5555", solicitud);
        
        if (respuesta != null) {
            boolean exito = (boolean) respuesta.get("exito");
            System.out.println(exito ? "✓ Operación exitosa" : "✗ Operación fallida");
            
            if (exito) {
                System.out.println("\n⏱️  Esperando 3 segundos para que se replique...");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // Estado después de replicación
                mostrarEstado("ESTADO DESPUÉS DE REPLICACIÓN");
                
                Map<String, Integer> finalSede1 = obtenerEstadoBD("SEDE1");
                Map<String, Integer> finalSede2 = obtenerEstadoBD("SEDE2");
                
                // Verificar replicación
                System.out.println("\n" + "=");
                System.out.println("  ANÁLISIS DE REPLICACIÓN");
                System.out.println("=");
                
                int cambioSede1 = finalSede1.get("prestamos") - inicialSede1.get("prestamos");
                int cambioSede2 = finalSede2.get("prestamos") - inicialSede2.get("prestamos");
                
                System.out.println("\nCambios detectados:");
                System.out.println("  SEDE1: " + cambioSede1 + " nuevo(s) préstamo(s)");
                System.out.println("  SEDE2: " + cambioSede2 + " nuevo(s) préstamo(s)");
                
                System.out.println("\n" + "=");
                if (cambioSede1 > 0 && cambioSede2 > 0) {
                    System.out.println("  ✓✓✓ REPLICACIÓN FUNCIONANDO CORRECTAMENTE ✓✓✓");
                    System.out.println("  La operación se registró en ambas sedes");
                } else if (cambioSede1 > 0 && cambioSede2 == 0) {
                    System.out.println("  ✗✗✗ REPLICACIÓN NO FUNCIONA ✗✗✗");
                    System.out.println("  La operación solo se registró en SEDE1");
                    System.out.println("\n  Posibles causas:");
                    System.out.println("  - SEDE2 no está suscrita al socket de replicación");
                    System.out.println("  - El puerto de replicación es incorrecto");
                    System.out.println("  - El método iniciarReceptorReplicas() no se ejecutó");
                } else {
                    System.out.println("  ⚠️  RESULTADO INESPERADO");
                    System.out.println("  Verifica que ambas sedes estén corriendo");
                }
                System.out.println("=");
            }
        } else {
            System.out.println("✗ No se pudo conectar con SEDE1");
            System.out.println("  Asegúrate de que SEDE1 esté corriendo en localhost:5555");
        }
    }

    public void monitorearContinuo(int intervalSegundos) {
        System.out.println("\n╔══════════════════════════════════════════════════════╗");
        System.out.println("║     MONITOR CONTINUO (Ctrl+C para salir)           ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        
        while (true) {
            mostrarEstado("ESTADO ACTUAL - " + new Date());
            
            try {
                System.out.println("\n⏱️  Actualizando en " + intervalSegundos + " segundos...\n");
                Thread.sleep(intervalSegundos * 1000);
            } catch (InterruptedException e) {
                System.out.println("\n✓ Monitor detenido");
                break;
            }
        }
    }

    public void cerrar() {
        context.close();
    }

    public static void main(String[] args) {
        VerificadorReplicacion verificador = new VerificadorReplicacion();
        
        try {
            if (args.length > 0 && args[0].equals("monitor")) {
                int intervalo = args.length > 1 ? Integer.parseInt(args[1]) : 5;
                verificador.monitorearContinuo(intervalo);
            } else {
                verificador.pruebaReplicacion();
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            verificador.cerrar();
        }
    }
}
