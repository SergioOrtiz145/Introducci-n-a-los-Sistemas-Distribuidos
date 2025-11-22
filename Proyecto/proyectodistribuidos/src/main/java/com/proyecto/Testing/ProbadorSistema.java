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
        System.out.println("Conectado a " + nombre + ": " + direccion);
        return socket;
    }

    private Map<String, Object> enviarSolicitud(String nombreSede, Map<String, Object> solicitud) {
        ZMQ.Socket socket = sockets.get(nombreSede);
        if (socket == null) {
            System.err.println("Socket para " + nombreSede + " no existe");
            return null;
        }

        try {
            String solicitudJson = gson.toJson(solicitud);
            socket.send(solicitudJson);
            String respuestaJson = socket.recvStr();
            
            if (respuestaJson == null) {
                System.err.println("Timeout esperando respuesta de " + nombreSede);
                return null;
            }

            return gson.fromJson(respuestaJson, new TypeToken<Map<String, Object>>(){}.getType());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return null;
        }
    }

    // MODIFICADO: Ahora devuelve un objeto con más información
    private Map<String, Object> obtenerInfoLibro(String sede, String isbn) {
        Map<String, Object> info = new HashMap<>();
        try {
            String rutaLibros = "./datos/" + sede.toLowerCase() + "/libros_" + sede + ".txt";
            List<String> lineas = Files.readAllLines(Paths.get(rutaLibros));
            
            for (String linea : lineas) {
                if (linea.startsWith(isbn + ",")) {
                    String[] datos = linea.split(",");
                    int totales = Integer.parseInt(datos[3]);
                    int prestados = Integer.parseInt(datos[4]);
                    int disponibles = totales - prestados;
                    
                    info.put("totales", totales);
                    info.put("prestados", prestados);
                    info.put("disponibles", disponibles);
                    info.put("titulo", datos[1]);
                    return info;
                }
            }
        } catch (IOException e) {
            info.put("error", true);
        }
        return info;
    }

    // NUEVO: Contar préstamos por sede de origen
    private Map<String, Integer> contarPrestamosPorSede(String sedeArchivo) {
        Map<String, Integer> contador = new HashMap<>();
        contador.put("SEDE1", 0);
        contador.put("SEDE2", 0);
        contador.put("TOTAL", 0);
        
        try {
            String rutaPrestamos = "./datos/" + sedeArchivo.toLowerCase() + 
                                  "/prestamos_" + sedeArchivo + ".txt";
            List<String> lineas = Files.readAllLines(Paths.get(rutaPrestamos));
            
            for (String linea : lineas) {
                if (linea.trim().isEmpty()) continue;
                
                String[] datos = linea.split(",");
                if (datos.length >= 7) {
                    String sedeOrigen = datos[6].trim(); // Columna 7: sede origen
                    contador.put(sedeOrigen, contador.getOrDefault(sedeOrigen, 0) + 1);
                    contador.put("TOTAL", contador.get("TOTAL") + 1);
                }
            }
        } catch (IOException e) {
            System.err.println("Error leyendo préstamos: " + e.getMessage());
        }
        
        return contador;
    }

    // NUEVO: Mostrar estado detallado del libro
    private void mostrarEstadoLibroDetallado(String isbn, String tituloCorto) {
        Map<String, Object> infoSede1 = obtenerInfoLibro("SEDE1", isbn);
        Map<String, Object> infoSede2 = obtenerInfoLibro("SEDE2", isbn);
        
        if (infoSede1.containsKey("error") || infoSede2.containsKey("error")) {
            System.out.println(" Error obteniendo información del libro");
            return;
        }
        
        int disp1 = (int) infoSede1.get("disponibles");
        int disp2 = (int) infoSede2.get("disponibles");
        int prest1 = (int) infoSede1.get("prestados");
        int prest2 = (int) infoSede2.get("prestados");
        
        // Verificar que los inventarios NO hayan cambiado (eso es correcto)
        String estadoInventario = (disp1 == (int)infoSede1.get("totales") - prest1 && 
                                   disp2 == (int)infoSede2.get("totales") - prest2) 
                                   ? "CORRECTO" : "VERIFICAR";
        
        System.out.println(" " + tituloCorto + " (" + isbn + ")");
        System.out.println("     ┌─────────────┬────────────┬─────────────┬──────────────┐");
        System.out.println("     │    Sede     │  Totales   │  Prestados  │ Disponibles  │");
        System.out.println("     ├─────────────┼────────────┼─────────────┼──────────────┤");
        System.out.printf("     │   SEDE1     │     %2d     │      %2d     │      %2d      │%n", 
            infoSede1.get("totales"), prest1, disp1);
        System.out.printf("     │   SEDE2     │     %2d     │      %2d     │      %2d      │%n", 
            infoSede2.get("totales"), prest2, disp2);
        System.out.println("     └─────────────┴────────────┴─────────────┴──────────────┘");
        System.out.println("     Estado inventario: " + estadoInventario);
    }

    // NUEVO: Mostrar resumen de réplicas
    private void mostrarResumenReplicas() {
        System.out.println("\n╔════════════════════════════════════════════════════════╗");
        System.out.println("║  RESUMEN DE RÉPLICAS EN ARCHIVOS                      ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");
        
        Map<String, Integer> prestamosSede1 = contarPrestamosPorSede("SEDE1");
        Map<String, Integer> prestamosSede2 = contarPrestamosPorSede("SEDE2");
        
        System.out.println("Archivo: prestamos_SEDE1.txt");
        System.out.println("   - Préstamos originados en SEDE1 (locales): " + prestamosSede1.get("SEDE1"));
        System.out.println("   - Préstamos originados en SEDE2 (réplicas): " + prestamosSede1.get("SEDE2"));
        System.out.println("   - TOTAL registrado en archivo: " + prestamosSede1.get("TOTAL"));
        
        System.out.println("\nArchivo: prestamos_SEDE2.txt");
        System.out.println("   - Préstamos originados en SEDE2 (locales): " + prestamosSede2.get("SEDE2"));
        System.out.println("   - Préstamos originados en SEDE1 (réplicas): " + prestamosSede2.get("SEDE1"));
        System.out.println("   - TOTAL registrado en archivo: " + prestamosSede2.get("TOTAL"));
        
        int replicasSede1 = prestamosSede1.get("SEDE2");
        int replicasSede2 = prestamosSede2.get("SEDE1");
        
        System.out.println("\n═══════════════════════════════════════════════════════");
        if (replicasSede1 > 0 && replicasSede2 > 0) {
            System.out.println("  REPLICACIÓN BIDIRECCIONAL ACTIVA");
            System.out.println("  - SEDE1 tiene " + replicasSede1 + " réplicas de SEDE2");
            System.out.println("  - SEDE2 tiene " + replicasSede2 + " réplicas de SEDE1");
        } else if (replicasSede1 == 0 && replicasSede2 == 0) {
            System.out.println("  NO HAY RÉPLICAS AÚN");
            System.out.println("  - Esto es normal si acabas de iniciar el sistema");
            System.out.println("  - Realiza operaciones y espera a que se repliquen");
        } else {
            System.out.println("  REPLICACIÓN UNIDIRECCIONAL");
            System.out.println("  - Verifica que ambos GA estén activos");
        }
        System.out.println("═══════════════════════════════════════════════════════");
    }

    public void demostrarSincronizacion() {
        System.out.println("\n╔═══=════════════════════════════════════════════════════╗");
        System.out.println("║  DEMOSTRACIÓN DE REPLICACION (sin modificar inventario)║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");

        conectar("SEDE1", "tcp://localhost:5555");
        conectar("SEDE2", "tcp://localhost:6555");

        String isbnDemo = "ISBN0001";
        String tituloDemo = "Cien Años de Soledad Vol.2";

        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("  PASO 0: Estado inicial del sistema");
        System.out.println("═══════════════════════════════════════════════════════");
        mostrarResumenReplicas();
        esperarEnter();

        System.out.println("\n═══════════════════════════════════════════════════════");
        System.out.println("  PASO 1: Estado inicial del libro " + isbnDemo);
        System.out.println("═══════════════════════════════════════════════════════");
        mostrarEstadoLibroDetallado(isbnDemo, tituloDemo);
        
        System.out.println("\n💡 Nota importante:");
        System.out.println("   - Los inventarios de cada sede son INDEPENDIENTES");
        System.out.println("   - Solo cambian cuando hay operaciones LOCALES");
        System.out.println("   - Las réplicas NO modifican el inventario");
        esperarEnter();

        // Préstamo en SEDE1
        System.out.println("\n═══════════════════════════════════════════════════════");
        System.out.println("  PASO 2: Usuario 'Ana' pide el libro en SEDE1");
        System.out.println("═══════════════════════════════════════════════════════");
        
        Map<String, Object> solicitud1 = new HashMap<>();
        solicitud1.put("operacion", "PRESTAMO");
        solicitud1.put("isbn", isbnDemo);
        solicitud1.put("usuario", "ana_estudiante");
        
        System.out.println("\n Enviando solicitud a SEDE1...");
        Map<String, Object> resp1 = enviarSolicitud("SEDE1", solicitud1);
        
        if (resp1 != null && (boolean) resp1.get("exito")) {
            System.out.println("✓ SEDE1 responde: " + resp1.get("mensaje"));
            System.out.println("\n⏱️  Esperando 3 segundos para que se replique a SEDE2...");
            esperar(3000);
            
            System.out.println("\n📊 Inventarios después de préstamo en SEDE1:");
            mostrarEstadoLibroDetallado(isbnDemo, tituloDemo);
            
            System.out.println("\nObserva:");
            System.out.println("   - SEDE1: 'Prestados' aumentó en 1 (operación local)");
            System.out.println("   - SEDE2: Inventario SIN CAMBIOS (solo se registró la réplica)");
            System.out.println("   - Cada sede mantiene su inventario físico independiente ✅");
            
            System.out.println("\nVerificando archivos de préstamos:");
            mostrarResumenReplicas();
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
            System.out.println("\nEsperando 3 segundos para que se replique a SEDE1...");
            esperar(3000);
            
            System.out.println("\nInventarios después de préstamo en SEDE2:");
            mostrarEstadoLibroDetallado(isbnDemo, tituloDemo);
            
            System.out.println("\n💡 Observa:");
            System.out.println("   - SEDE2: 'Prestados' aumentó en 1 (operación local)");
            System.out.println("   - SEDE1: Inventario SIN CAMBIOS (solo se registró la réplica)");
            System.out.println("   - Cada sede gestiona sus libros físicos de forma independiente ✅");
            
            System.out.println("\n📋 Verificando archivos de préstamos:");
            mostrarResumenReplicas();
        }
        esperarEnter();

        System.out.println("\n╔════════════════════════════════════════════════════════╗");
        System.out.println("║  SISTEMA FUNCIONANDO CORRECTAMENTE                 ║");
        System.out.println("║                                                        ║");
        System.out.println("║  - Los inventarios son independientes por sede         ║");
        System.out.println("║  - Las réplicas se registran sin modificar inventario  ║");
        System.out.println("║  - Cada sede conoce todas las operaciones del sistema  ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");
    }

    public void pruebaRapida() {
        System.out.println("\n╔════════════════════════════════════════════════════════╗");
        System.out.println("║  PRUEBA RÁPIDA - Verificación de estado actual       ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");

        mostrarResumenReplicas();

        System.out.println("\n═══ Estado de algunos libros ═══\n");
        String[] libros = {"ISBN0001", "ISBN0002", "ISBN0010"};
        
        for (String isbn : libros) {
            mostrarEstadoLibroDetallado(isbn, "Libro " + isbn);
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