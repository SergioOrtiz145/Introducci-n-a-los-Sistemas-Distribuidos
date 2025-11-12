package com.proyecto.Persistencia;

import com.proyecto.Modelos.Libro;
import com.proyecto.Modelos.Prestamo;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BaseDatos {
    private final String rutaLibros;
    private final String rutaPrestamos;
    private final Map<String, Libro> libros;
    private final Map<String, Prestamo> prestamos;
    private final Object lockArchivo = new Object();
    private final String sede;
    
    public BaseDatos(String rutaBase, String sede) {
        this.sede = sede;
        this.rutaLibros = rutaBase + "/libros_" + sede + ".txt";
        this.rutaPrestamos = rutaBase + "/prestamos_" + sede + ".txt";
        this.libros = new ConcurrentHashMap<>();
        this.prestamos = new ConcurrentHashMap<>();
        inicializarArchivos();
        cargarDatos();
    }
    
    private void inicializarArchivos() {
        try {
            Path directorio = Paths.get(rutaLibros).getParent();
            if (directorio != null && !Files.exists(directorio)) {
                Files.createDirectories(directorio);
            }
            
            if (!Files.exists(Paths.get(rutaLibros))) {
                Files.createFile(Paths.get(rutaLibros));
            }
            if (!Files.exists(Paths.get(rutaPrestamos))) {
                Files.createFile(Paths.get(rutaPrestamos));
            }
        } catch (IOException e) {
            System.err.println("Error inicializando archivos: " + e.getMessage());
        }
    }
    
    private void cargarDatos() {
        cargarLibros();
        cargarPrestamos();
        System.out.println("BD " + sede + " cargada: " + libros.size() + " libros, " 
                          + prestamos.size() + " préstamos");
    }
    
    private void cargarLibros() {
        try {
            List<String> lineas = Files.readAllLines(Paths.get(rutaLibros));
            for (String linea : lineas) {
                if (linea.trim().isEmpty()) continue;
                
                String[] datos = linea.split(",");
                if (datos.length >= 5) {
                    Libro libro = new Libro(
                        datos[0].trim(), 
                        datos[1].trim(), 
                        datos[2].trim(), 
                        Integer.parseInt(datos[3].trim())
                    );
                    libros.put(libro.getIsbn(), libro);
                }
            }
        } catch (IOException e) {
            System.err.println("Error cargando libros: " + e.getMessage());
        }
    }
    
    private void cargarPrestamos() {
        try {
            List<String> lineas = Files.readAllLines(Paths.get(rutaPrestamos));
            for (String linea : lineas) {
                if (linea.trim().isEmpty()) continue;
                
                String[] datos = linea.split(",");
                if (datos.length >= 6) {
                    Prestamo prestamo = new Prestamo(
                        datos[0].trim(), 
                        datos[1].trim(), 
                        datos[2].trim(), 
                        datos[5].trim()
                    );
                    prestamos.put(prestamo.getIdPrestamo(), prestamo);
                }
            }
        } catch (IOException e) {
            System.err.println("Error cargando préstamos: " + e.getMessage());
        }
    }
    
    public synchronized boolean realizarPrestamo(String isbn, String usuario) {
        Libro libro = libros.get(isbn);
        if (libro != null && libro.prestar()) {
            String idPrestamo = UUID.randomUUID().toString();
            Prestamo prestamo = new Prestamo(idPrestamo, isbn, usuario, sede);
            prestamos.put(idPrestamo, prestamo);
            persistirCambios();
            System.out.println("Préstamo realizado: " + idPrestamo + " - " + libro.getTitulo());
            return true;
        }
        return false;
    }
    
    public synchronized boolean realizarDevolucion(String idPrestamo) {
        Prestamo prestamo = prestamos.get(idPrestamo);
        if (prestamo != null && prestamo.isPrestamoActivo()) {
            Libro libro = libros.get(prestamo.getIsbn());
            if (libro != null) {
                libro.devolver();
                prestamo.setPrestamoActivo(false);
                prestamo.setFechaDevolucion(LocalDateTime.now());
                prestamos.remove(idPrestamo);
                persistirCambios();
                System.out.println("Devolución realizada: " + idPrestamo);
                return true;
            }
        }
        return false;
    }
    
    public synchronized boolean realizarRenovacion(String idPrestamo) {
        Prestamo prestamo = prestamos.get(idPrestamo);
        if (prestamo != null && prestamo.puedeRenovarse()) {
            prestamo.setNumRenovaciones(prestamo.getNumRenovaciones() + 1);
            prestamo.setFechaPrestamo(LocalDateTime.now());
            persistirCambios();
            System.out.println("Renovación realizada: " + idPrestamo + 
                             " (Renovación #" + prestamo.getNumRenovaciones() + ")");
            return true;
        }
        return false;
    }
    
    private void persistirCambios() {
        synchronized (lockArchivo) {
            try {
                // Guardar libros
                List<String> lineasLibros = new ArrayList<>();
                for (Libro libro : libros.values()) {
                    lineasLibros.add(libro.toCSV());
                }
                Files.write(Paths.get(rutaLibros), lineasLibros, 
                           StandardOpenOption.TRUNCATE_EXISTING);
                
                // Guardar préstamos activos
                List<String> lineasPrestamos = new ArrayList<>();
                for (Prestamo prestamo : prestamos.values()) {
                    lineasPrestamos.add(prestamo.toCSV());
                }
                Files.write(Paths.get(rutaPrestamos), lineasPrestamos, 
                           StandardOpenOption.TRUNCATE_EXISTING);
                
            } catch (IOException e) {
                System.err.println("Error persistiendo datos: " + e.getMessage());
                throw new RuntimeException("Fallo crítico en persistencia", e);
            }
        }
    }
    
    public Map<String, Libro> getLibros() {
        return new HashMap<>(libros);
    }
    
    public Map<String, Prestamo> getPrestamos() {
        return new HashMap<>(prestamos);
    }
    
    public Libro consultarLibro(String isbn) {
        return libros.get(isbn);
    }
    
    public Prestamo consultarPrestamo(String idPrestamo) {
        return prestamos.get(idPrestamo);
    }
    
    public boolean verificarDisponibilidad() {
        try {
            return Files.exists(Paths.get(rutaLibros)) && 
                   Files.isReadable(Paths.get(rutaLibros)) &&
                   Files.isWritable(Paths.get(rutaLibros));
        } catch (Exception e) {
            return false;
        }
    }
    
    public String getSede() {
        return sede;
    }
}

