package com.proyecto.Testing;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Genera datos iniciales para el sistema de préstamo de libros
 * Según requisitos: 1000 libros, 200 prestados (50 SEDE1, 150 SEDE2)
 */
public class GeneradorDatosIniciales {

    private static final String[] TITULOS = {
            "Don Quijote de la Mancha", "Cien Años de Soledad", "1984", "Rayuela",
            "El Aleph", "La Odisea", "Hamlet", "Crimen y Castigo", "En Busca del Tiempo Perdido",
            "Ulises", "La Metamorfosis", "El Gran Gatsby", "Lolita", "Mrs. Dalloway",
            "Orgullo y Prejuicio", "Madame Bovary", "Guerra y Paz", "Los Miserables",
            "El Extranjero", "El Señor de los Anillos", "Harry Potter", "Fundación",
            "Dune", "Neuromante", "El Hobbit", "Crónica de una Muerte Anunciada",
            "La Casa de los Espíritus", "Pedro Páramo", "El Túnel", "Ficciones"
    };

    private static final String[] AUTORES = {
            "Miguel de Cervantes", "Gabriel García Márquez", "George Orwell", "Julio Cortázar",
            "Jorge Luis Borges", "Homero", "William Shakespeare", "Fiódor Dostoyevski",
            "Marcel Proust", "James Joyce", "Franz Kafka", "F. Scott Fitzgerald",
            "Vladimir Nabokov", "Virginia Woolf", "Jane Austen", "Gustave Flaubert",
            "León Tolstói", "Victor Hugo", "Albert Camus", "J.R.R. Tolkien",
            "J.K. Rowling", "Isaac Asimov", "Frank Herbert", "William Gibson",
            "Isabel Allende", "Juan Rulfo", "Ernesto Sabato", "Octavio Paz"
    };

    private static final String[] USUARIOS = {
            "est001", "est002", "est003", "est004", "est005", "est006", "est007", "est008",
            "prof001", "prof002", "prof003", "prof004", "prof005",
            "inv001", "inv002", "inv003"
    };

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        String rutaBase = args.length > 0 ? args[0] : "./datos";

        System.out.println("═══════════════════════════════════════════");
        System.out.println("  GENERADOR DE DATOS INICIALES");
        System.out.println("═══════════════════════════════════════════");
        System.out.println("Ruta base: " + rutaBase);

        try {
            // Crear directorios
            crearDirectorios(rutaBase);

            // Generar libros (iguales para ambas sedes)
            List<String> libros = generarLibros(1000);

            // Generar préstamos diferentes por sede
            List<String> prestamosSede1 = generarPrestamos(libros, 50, "SEDE1");
            List<String> prestamosSede2 = generarPrestamos(libros, 150, "SEDE2");

            // Ajustar disponibilidad de libros según préstamos
            List<String> librosSede1 = ajustarDisponibilidad(libros, prestamosSede1);
            List<String> librosSede2 = ajustarDisponibilidad(libros, prestamosSede2);

            // Guardar archivos SEDE1
            guardarArchivo(rutaBase + "/sede1/libros_SEDE1.txt", librosSede1);
            guardarArchivo(rutaBase + "/sede1/prestamos_SEDE1.txt", prestamosSede1);

            // Guardar archivos SEDE2
            guardarArchivo(rutaBase + "/sede2/libros_SEDE2.txt", librosSede2);
            guardarArchivo(rutaBase + "/sede2/prestamos_SEDE2.txt", prestamosSede2);

            System.out.println("\n✓ Datos generados exitosamente");
            System.out.println("  - 1000 libros por sede");
            System.out.println("  - 50 préstamos en SEDE1");
            System.out.println("  - 150 préstamos en SEDE2");
            System.out.println("═══════════════════════════════════════════");

        } catch (Exception e) {
            System.err.println("✗ Error generando datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void crearDirectorios(String rutaBase) throws IOException {
        Files.createDirectories(Paths.get(rutaBase + "/sede1"));
        Files.createDirectories(Paths.get(rutaBase + "/sede2"));
        System.out.println("✓ Directorios creados");
    }

    private static List<String> generarLibros(int cantidad) {
        List<String> libros = new ArrayList<>();
        Random random = new Random(12345); // Seed fijo para reproducibilidad

        System.out.println("\nGenerando " + cantidad + " libros...");

        for (int i = 1; i <= cantidad; i++) {
            String isbn = String.format("ISBN%04d", i);
            String titulo = TITULOS[random.nextInt(TITULOS.length)] + " Vol." + ((i % 10) + 1);
            String autor = AUTORES[random.nextInt(AUTORES.length)];

            // Distribución de ejemplares:
            // - 60% tienen 2-5 ejemplares
            // - 30% tienen 1 ejemplar (único)
            // - 10% tienen 6-10 ejemplares
            int ejemplares;
            double prob = random.nextDouble();
            if (prob < 0.3) {
                ejemplares = 1; // 30% libros únicos
            } else if (prob < 0.9) {
                ejemplares = 2 + random.nextInt(4); // 60% entre 2-5
            } else {
                ejemplares = 6 + random.nextInt(5); // 10% entre 6-10
            }

            // Formato CSV: ISBN,Titulo,Autor,EjemplaresTotal
            libros.add(String.format("%s,%s,%s,%d", isbn, titulo, autor, ejemplares));

            if (i % 100 == 0) {
                System.out.println("  Generados " + i + " libros...");
            }
        }

        System.out.println("✓ " + cantidad + " libros generados");
        return libros;
    }

    private static List<String> generarPrestamos(List<String> libros, int cantidad, String sede) {
        List<String> prestamos = new ArrayList<>();
        Random random = new Random(sede.hashCode()); // Seed diferente por sede
        Set<String> isbnsUsados = new HashSet<>();

        System.out.println("\nGenerando " + cantidad + " préstamos para " + sede + "...");

        int intentos = 0;
        int prestamosCreados = 0;

        while (prestamosCreados < cantidad && intentos < cantidad * 3) {
            intentos++;

            // Seleccionar libro aleatorio
            String lineaLibro = libros.get(random.nextInt(libros.size()));
            String[] datosLibro = lineaLibro.split(",");
            String isbn = datosLibro[0];
            int ejemplaresTotal = Integer.parseInt(datosLibro[3]);

            // Solo prestar si tiene ejemplares disponibles y no está todo prestado
            int vecesPrestado = Collections.frequency(new ArrayList<>(isbnsUsados), isbn);
            if (vecesPrestado < ejemplaresTotal) {
                String idPrestamo = "PREST-" + sede + "-" + String.format("%04d", prestamosCreados + 1);
                String usuario = USUARIOS[random.nextInt(USUARIOS.length)];

                // Fecha de préstamo: últimos 14 días
                LocalDateTime fechaPrestamo = LocalDateTime.now()
                        .minusDays(random.nextInt(14));

                // Renovaciones: 70% sin renovar, 20% una vez, 10% dos veces
                int renovaciones = random.nextDouble() < 0.7 ? 0 : (random.nextDouble() < 0.67 ? 1 : 2);

                String prestamoActivo = "true";

                // Formato CSV:
                // idPrestamo,isbn,usuario,fechaPrestamo,renovaciones,prestamoActivo
                prestamos.add(String.format("%s,%s,%s,%s,%d,%s,%s",
                        idPrestamo, isbn, usuario, fechaPrestamo, renovaciones, prestamoActivo,
                        sede));

                isbnsUsados.add(isbn);
                prestamosCreados++;
            }
        }

        System.out.println("✓ " + prestamosCreados + " préstamos generados para " + sede);
        return prestamos;
    }

    private static List<String> ajustarDisponibilidad(List<String> libros, List<String> prestamos) {
        // Contar cuántas veces se prestó cada libro
        Map<String, Integer> contadorPrestamos = new HashMap<>();
        for (String prestamo : prestamos) {
            String[] datos = prestamo.split(",");
            String isbn = datos[1];
            contadorPrestamos.put(isbn, contadorPrestamos.getOrDefault(isbn, 0) + 1);
        }

        // Ajustar disponibilidad
        List<String> librosAjustados = new ArrayList<>();
        for (String libro : libros) {
            String[] datos = libro.split(",");
            String isbn = datos[0];
            int ejemplaresTotal = Integer.parseInt(datos[3]);
            int ejemplaresPrestados = contadorPrestamos.getOrDefault(isbn, 0);

            // Formato final: ISBN,Titulo,Autor,EjemplaresTotal,EjemplaresPrestados
            librosAjustados.add(libro + "," + ejemplaresPrestados);
        }

        return librosAjustados;
    }

    private static void guardarArchivo(String ruta, List<String> lineas) throws IOException {
        Path path = Paths.get(ruta);

        // Crear directorio padre si no existe
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        Files.write(path, lineas, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);

        System.out.println("✓ Archivo guardado: " + ruta + " (" + lineas.size() + " líneas)");
    }
}
