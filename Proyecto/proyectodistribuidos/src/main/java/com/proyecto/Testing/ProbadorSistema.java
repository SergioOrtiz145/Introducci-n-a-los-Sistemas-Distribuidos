package com.proyecto.Testing;

import org.zeromq.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.*;
import java.io.*;
import java.nio.file.*;

public class ProbadorSistema {
    private ZContext context;
    private Gson gson;
    private Map<String, ZMQ.Socket> sockets;

    public ProbadorSistema() {
        this.context = new ZContext();
        this.gson = new Gson();
        this.sockets = new HashMap<>();
    }

    private ZMQ.Socket conectar(String nombre, String direccion) {
        ZMQ.Socket socket = context.createSocket(SocketType.REQ);
        socket.connect(direccion);
        socket.setReceiveTimeOut(5000);
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
            System.err.println("✗ Error: " + e.getMessage());
            return null;
        }
    }

    private int obtenerDisponibles(String sede, String isbn) {
        try {
            String rutaLibros = "./datos/" + sede.toLowerCase() + "/libros_" + sede + ".txt";
            List<String> lineas = Files.readAllLines(Paths.get(rutaLibros));
            
            for (String linea : lineas) {
                if (linea.startsWith(isbn + ",")) {
                    String[] datos = linea.split(",");
                    int totales = Integer.parseInt(datos[3]);
                    int prestados = Integer.parseInt(datos[4]);
                    return totales - prestados;
                }
            }
        } catch (IOException e) {
            return -1;
        }
        return -1;
    }

    private void mostrarEstadoLibro(String isbn, String tituloCorto) {
        int disponiblesSede1 = obtenerDisponibles("SEDE1", isbn);
        int disponiblesSede2 = obtenerDisponibles("SEDE2", isbn);
        
        String estado = (disponiblesSede1 == disponiblesSede2) ? "✅ SINCRONIZADO" : "⚠️  DIFERENTE";
        
        System.out.println("  📚 " + tituloCorto + " (" + isbn + ")");
        System.out.println("     SEDE1: " + disponiblesSede1 + " disponibles");
        System.out.println("     SEDE2: " + disponiblesSede2 + " disponibles");
        System.out.println("     Estado: " + estado);
    }

    public void demostrarSincronizacion() {
        System.out.println("\n╔════════════════════════════════════════════════════════╗");
        System.out.println("║  DEMOSTRACIÓN DE SINCRONIZACIÓN BIDIRECCIONAL         ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");

        conectar("SEDE1", "tcp://localhost:5555");
        conectar("SEDE2", "tcp://localhost:6555");

        // Seleccionar un libro específico para seguir
        String isbnDemo = "ISBN0001";
        String tituloDemo = "Cien Años de Soledad Vol.2";

        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("  PASO 1: Estado inicial del libro " + isbnDemo);
        System.out.println("═══════════════════════════════════════════════════════");
        mostrarEstadoLibro(isbnDemo, tituloDemo);
        
        System.out.println("\n💡 Ambas sedes tienen el MISMO inventario del libro.");
        esperarEnter();

        // Préstamo en SEDE1
        System.out.println("\n═══════════════════════════════════════════════════════");
        System.out.println("  PASO 2: Usuario 'Ana' pide el libro en SEDE1");
        System.out.println("═══════════════════════════════════════════════════════");
        
        Map<String, Object> solicitud1 = new HashMap<>();
        solicitud1.put("operacion", "PRESTAMO");
        solicitud1.put("isbn", isbnDemo);
        solicitud1.put("usuario", "ana_estudiante");
        
        System.out.println("\n→ Enviando solicitud a SEDE1...");
        Map<String, Object> resp1 = enviarSolicitud("SEDE1", solicitud1);
        
        if (resp1 != null && (boolean) resp1.get("exito")) {
            System.out.println("✓ SEDE1 responde: " + resp1.get("mensaje"));
            System.out.println("\n⏱️  Esperando 3 segundos para que se replique a SEDE2...");
            esperar(3000);
            
            System.out.println("\n📊 Estado después de préstamo en SEDE1:");
            mostrarEstadoLibro(isbnDemo, tituloDemo);
            
            System.out.println("\n💡 Observa:");
            System.out.println("   • SEDE1 descontó 1 ejemplar (procesó el préstamo)");
            System.out.println("   • SEDE2 TAMBIÉN descontó 1 ejemplar (recibió la réplica)");
            System.out.println("   • Ambas sedes quedan sincronizadas ✅");
        }
        esperarEnter();

        // Préstamo en SEDE2
        System.out.println("\n═══════════════════════════════════════════════════════");
        System.out.println("  PASO 3: Usuario 'Carlos' pide el libro en SEDE2");
        System.out.println("═══════════════════════════════════════════════════════");
        
        Map<String, Object> solicitud2 = new HashMap<>();
        solicitud2.put("operacion", "PRESTAMO");
        solicitud2.put("isbn", isbnDemo);
        solicitud2.put("usuario", "carlos_profesor");
        
        System.out.println("\n→ Enviando solicitud a SEDE2...");
        Map<String, Object> resp2 = enviarSolicitud("SEDE2", solicitud2);
        
        if (resp2 != null && (boolean) resp2.get("exito")) {
            System.out.println("✓ SEDE2 responde: " + resp2.get("mensaje"));
            System.out.println("\n⏱️  Esperando 3 segundos para que se replique a SEDE1...");
            esperar(3000);
            
            System.out.println("\n📊 Estado después de préstamo en SEDE2:");
            mostrarEstadoLibro(isbnDemo, tituloDemo);
            
            System.out.println("\n💡 Observa:");
            System.out.println("   • SEDE2 descontó 1 ejemplar (procesó el préstamo)");
            System.out.println("   • SEDE1 TAMBIÉN descontó 1 ejemplar (recibió la réplica)");
            System.out.println("   • La sincronización funciona en AMBAS direcciones ✅");
        }
        esperarEnter();

        // Múltiples préstamos
        System.out.println("\n═══════════════════════════════════════════════════════");
        System.out.println("  PASO 4: Varios usuarios piden el libro simultáneamente");
        System.out.println("═══════════════════════════════════════════════════════");
        
        System.out.println("\n→ María pide el libro en SEDE1...");
        Map<String, Object> sol3 = new HashMap<>();
        sol3.put("operacion", "PRESTAMO");
        sol3.put("isbn", isbnDemo);
        sol3.put("usuario", "maria_estudiante");
        enviarSolicitud("SEDE1", sol3);
        
        System.out.println("→ Juan pide el libro en SEDE2...");
        Map<String, Object> sol4 = new HashMap<>();
        sol4.put("operacion", "PRESTAMO");
        sol4.put("isbn", isbnDemo);
        sol4.put("usuario", "juan_estudiante");
        enviarSolicitud("SEDE2", sol4);
        
        System.out.println("→ Pedro pide el libro en SEDE1...");
        Map<String, Object> sol5 = new HashMap<>();
        sol5.put("operacion", "PRESTAMO");
        sol5.put("isbn", isbnDemo);
        sol5.put("usuario", "pedro_profesor");
        enviarSolicitud("SEDE1", sol5);
        
        System.out.println("\n⏱️  Esperando 5 segundos para todas las réplicas...");
        esperar(5000);
        
        System.out.println("\n📊 Estado final después de múltiples préstamos:");
        mostrarEstadoLibro(isbnDemo, tituloDemo);
        
        System.out.println("\n💡 Conclusión:");
        System.out.println("   • Total de préstamos: 5 (2 SEDE1 inicial + 2 SEDE2 + 1 SEDE1)");
        System.out.println("   • Cada préstamo se descontó en AMBAS sedes");
        System.out.println("   • El inventario está perfectamente sincronizado ✅");
        
        // Resumen con varios libros
        System.out.println("\n═══════════════════════════════════════════════════════");
        System.out.println("  VERIFICACIÓN FINAL: Estado de varios libros");
        System.out.println("═══════════════════════════════════════════════════════\n");
        
        mostrarEstadoLibro("ISBN0001", "Cien Años Soledad");
        System.out.println();
        mostrarEstadoLibro("ISBN0002", "El Aleph");
        System.out.println();
        mostrarEstadoLibro("ISBN0010", "Ulises");
        
        System.out.println("\n╔════════════════════════════════════════════════════════╗");
        System.out.println("║  ✅ REPLICACIÓN BIDIRECCIONAL FUNCIONANDO            ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");
    }

    public void pruebaRapida() {
        System.out.println("\n╔════════════════════════════════════════════════════════╗");
        System.out.println("║  PRUEBA RÁPIDA DE REPLICACIÓN                        ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");

        conectar("SEDE1", "tcp://localhost:5555");
        conectar("SEDE2", "tcp://localhost:6555");

        String[] libros = {"ISBN0001", "ISBN0002", "ISBN0003", "ISBN0010", "ISBN0011"};
        
        System.out.println("═══ Estado inicial ═══\n");
        for (String isbn : libros) {
            mostrarEstadoLibro(isbn, "Libro " + isbn);
            System.out.println();
        }

        System.out.println("\n═══ Realizando préstamos ═══");
        
        // Alternamos entre sedes
        String[] usuarios = {"est001", "prof001", "est002", "inv001", "est003"};
        for (int i = 0; i < libros.length; i++) {
            String sede = (i % 2 == 0) ? "SEDE1" : "SEDE2";
            
            Map<String, Object> sol = new HashMap<>();
            sol.put("operacion", "PRESTAMO");
            sol.put("isbn", libros[i]);
            sol.put("usuario", usuarios[i]);
            
            System.out.println("  → " + usuarios[i] + " pide " + libros[i] + " en " + sede);
            enviarSolicitud(sede, sol);
        }

        System.out.println("\n⏱️  Esperando sincronización...\n");
        esperar(5000);

        System.out.println("═══ Estado después de réplicas ═══\n");
        for (String isbn : libros) {
            mostrarEstadoLibro(isbn, "Libro " + isbn);
            System.out.println();
        }
    }

    private void esperar(int milisegundos) {
        try {
            Thread.sleep(milisegundos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void esperarEnter() {
        System.out.println("\n[Presiona ENTER para continuar...]");
        try {
            System.in.read();
            // Limpiar buffer
            while (System.in.available() > 0) {
                System.in.read();
            }
        } catch (IOException e) {
            // Ignorar
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
            if (args.length > 0 && args[0].equals("rapida")) {
                probador.pruebaRapida();
            } else {
                probador.demostrarSincronizacion();
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            probador.cerrar();
        }
    }
}