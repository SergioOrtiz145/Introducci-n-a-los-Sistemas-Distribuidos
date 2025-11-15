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
    private final Map<String, Prestamo> prestamos; // Incluye locales Y remotos
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
                + prestamos.size() + " préstamos (locales + remotos)");
    }

    private void cargarLibros() {
        try {
            List<String> lineas = Files.readAllLines(Paths.get(rutaLibros));
            for (String linea : lineas) {
                if (linea.trim().isEmpty())
                    continue;

                String[] datos = linea.split(",");
                if (datos.length >= 4) {
                    Libro libro;

                    if (datos.length >= 5) {
                        libro = new Libro(
                                datos[0].trim(),
                                datos[1].trim(),
                                datos[2].trim(),
                                Integer.parseInt(datos[3].trim()),
                                Integer.parseInt(datos[4].trim()));
                    } else {
                        libro = new Libro(
                                datos[0].trim(),
                                datos[1].trim(),
                                datos[2].trim(),
                                Integer.parseInt(datos[3].trim()));
                    }

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
                if (linea.trim().isEmpty())
                    continue;

                String[] datos = linea.split(",");
                if (datos.length >= 6) {
                    String idPrestamo = datos[0].trim();
                    String isbn = datos[1].trim();
                    String usuario = datos[2].trim();
                    String fechaStr = datos[3].trim();

                    int renovaciones = 0;
                    if (!datos[4].trim().isEmpty()) {
                        try {
                            renovaciones = Integer.parseInt(datos[4].trim());
                        } catch (NumberFormatException e) {
                            renovaciones = 0;
                        }
                    }

                    boolean activo = true;
                    if (!datos[5].trim().isEmpty()) {
                        activo = Boolean.parseBoolean(datos[5].trim());
                    }

                    // Detectar si es préstamo local o remoto por el formato del ID
                    String sedeOrigen = sede; // Por defecto, asumimos que es local
                    if (datos.length >= 7) {
                        sedeOrigen = datos[6].trim(); // Si hay 7ª columna, indica la sede origen
                    }

                    Prestamo prestamo = new Prestamo(idPrestamo, isbn, usuario, sedeOrigen);
                    prestamo.setNumRenovaciones(renovaciones);
                    prestamo.setPrestamoActivo(activo);

                    try {
                        prestamo.setFechaPrestamo(LocalDateTime.parse(fechaStr));
                    } catch (Exception e) {
                        // Si falla el parseo, usar fecha actual
                    }

                    prestamos.put(prestamo.getIdPrestamo(), prestamo);
                }
            }
        } catch (IOException e) {
            System.err.println("Error cargando préstamos: " + e.getMessage());
        }
    }

    // NUEVO: Cargar operaciones remotas (ya no se usa, se eliminó)
    // Los préstamos remotos ahora se cargan en cargarPrestamos()

    public synchronized String realizarPrestamo(String isbn, String usuario) {
        Libro libro = libros.get(isbn);
        if (libro != null && libro.prestar()) {
            String idPrestamo = UUID.randomUUID().toString();
            Prestamo prestamo = new Prestamo(idPrestamo, isbn, usuario, sede);
            prestamos.put(idPrestamo, prestamo);
            persistirCambios();
            System.out.println("✓ Préstamo LOCAL realizado: " + idPrestamo + " - " + libro.getTitulo());
            return idPrestamo;
        }
        System.out.println("✗ Libro " + isbn + " no disponible");
        return null;
    }

    /**
     * CLAVE: Este método SOLO registra la operación remota
     * NO modifica el inventario local de libros
     */
    public synchronized String realizarPrestamoReplica(String isbn, String usuario, String idPrestamo, String sedeOrigen) {
        // Verificar que la operación no exista ya
        if (prestamos.containsKey(idPrestamo)) {
            System.out.println("⚠️  Operación remota " + idPrestamo + " ya registrada, ignorando");
            return idPrestamo;
        }
        
        // Solo registrar la operación, NO modificar inventario
        Prestamo prestamoRemoto = new Prestamo(idPrestamo, isbn, usuario, sedeOrigen);
        prestamos.put(idPrestamo, prestamoRemoto);
        
        persistirCambios();
        System.out.println("📝 Operación remota REGISTRADA (sin modificar inventario): " + idPrestamo);
        return idPrestamo;
    }

    public synchronized boolean realizarDevolucion(String idPrestamo) {
        // Buscar en préstamos locales
        Prestamo prestamo = prestamos.get(idPrestamo);
        
        if (prestamo != null && prestamo.isPrestamoActivo()) {
            Libro libro = libros.get(prestamo.getIsbn());
            if (libro != null) {
                libro.devolver();
                prestamo.setPrestamoActivo(false);
                prestamo.setFechaDevolucion(LocalDateTime.now());
                prestamos.remove(idPrestamo);
                persistirCambios();
                System.out.println("✓ Devolución LOCAL realizada: " + idPrestamo);
                return true;
            }
        }
        
        return false;
    }

    /**
     * NUEVO: Registrar devolución remota (sin modificar inventario)
     */
    public synchronized boolean realizarDevolucionReplica(String idPrestamo) {
        Prestamo prestamo = prestamos.get(idPrestamo);
        
        if (prestamo != null && prestamo.isPrestamoActivo()) {
            // Solo marcar como inactivo, NO devolver al inventario
            prestamo.setPrestamoActivo(false);
            prestamo.setFechaDevolucion(LocalDateTime.now());
            prestamos.remove(idPrestamo);
            persistirCambios();
            System.out.println("📝 Devolución remota REGISTRADA (sin modificar inventario): " + idPrestamo);
            return true;
        }
        
        return false;
    }

    public synchronized boolean realizarRenovacion(String idPrestamo) {
        Prestamo prestamo = prestamos.get(idPrestamo);
        
        if (prestamo != null && prestamo.puedeRenovarse()) {
            prestamo.setNumRenovaciones(prestamo.getNumRenovaciones() + 1);
            prestamo.setFechaPrestamo(LocalDateTime.now());
            persistirCambios();
            System.out.println("✓ Renovación LOCAL realizada: " + idPrestamo +
                    " (Renovación #" + prestamo.getNumRenovaciones() + ")");
            return true;
        }
        
        return false;
    }

    /**
     * NUEVO: Registrar renovación remota (sin modificar inventario)
     */
    public synchronized boolean realizarRenovacionReplica(String idPrestamo) {
        Prestamo prestamo = prestamos.get(idPrestamo);
        
        if (prestamo != null && prestamo.puedeRenovarse()) {
            prestamo.setNumRenovaciones(prestamo.getNumRenovaciones() + 1);
            prestamo.setFechaPrestamo(LocalDateTime.now());
            persistirCambios();
            System.out.println("📝 Renovación remota REGISTRADA: " + idPrestamo);
            return true;
        }
        
        return false;
    }

    private void persistirCambios() {
        synchronized (lockArchivo) {
            try {
                // Guardar libros (inventario local)
                List<String> lineasLibros = new ArrayList<>();
                for (Libro libro : libros.values()) {
                    lineasLibros.add(libro.toCSV());
                }
                Files.write(Paths.get(rutaLibros), lineasLibros,
                        StandardOpenOption.TRUNCATE_EXISTING);

                // Guardar TODOS los préstamos (locales + remotos) con sede origen
                List<String> lineasPrestamos = new ArrayList<>();
                for (Prestamo prestamo : prestamos.values()) {
                    // Agregar la sede origen como 7ª columna
                    lineasPrestamos.add(prestamo.toCSV() + "," + prestamo.getSede());
                }
                Files.write(Paths.get(rutaPrestamos), lineasPrestamos,
                        StandardOpenOption.TRUNCATE_EXISTING);

            } catch (IOException e) {
                System.err.println("Error persistiendo datos: " + e.getMessage());
                throw new RuntimeException("Fallo crítico en persistencia", e);
            }
        }
    }

    // ELIMINADO: método persistirOperacionesRemotas()
    // Ya no es necesario, todo se guarda en persistirCambios()

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