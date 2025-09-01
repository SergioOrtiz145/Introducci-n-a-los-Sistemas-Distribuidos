import java.io.*;
import java.util.*;
//Clase para gestionar el archivo de la base de datos de la biblioteca.
//Se encarga de leer y escribir la información de los libros en un
//archivo de texto plano ('libros.txt').
public class GestorArchivo {
  //Nombre del archivo
  private static final String ARCHIVO = "libros.txt"; // CSV: isbn,titulo,cantidad
  //Lee los libros desde el archivo de texto
  public static List<Libro> leerLibros() {
    List<Libro> out = new ArrayList<>();
    File f = new File(ARCHIVO);

    // Si no existe, crear uno base
    if (!f.exists()) {
      try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
        bw.write("978-0134685991,Effective Java,3\n");
        bw.write("978-0596009205,Head First Java,2\n");
        bw.write("978-0321356680,Java Concurrency in Practice,1\n");
      } catch (IOException e) { e.printStackTrace(); }
    }
    // Leer el archivo línea por línea
    try (BufferedReader br = new BufferedReader(new FileReader(ARCHIVO))) {
      String linea;
      while ((linea = br.readLine()) != null) {
        if (linea.trim().isEmpty()) continue; // saltar líneas vacias
        String[] a = linea.split(",");
        if (a.length >= 3) {
          String isbn = a[0].trim();
          String titulo = a[1].trim();
          int cantidad = Integer.parseInt(a[2].trim());
          out.add(new Libro(isbn, titulo, cantidad));
        }
      }
    } catch (IOException e) { e.printStackTrace(); }
    return out;
  }

  //Guarda en el archivo de texto la lista de libro actualizada.
  public static void guardarLibros(List<Libro> libros) {
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(ARCHIVO))) {
      for (Libro l : libros) {
        bw.write(l.getIsbn() + "," + l.getTitulo() + "," + l.getCantidad());
        bw.newLine();
      }
    } catch (IOException e) { e.printStackTrace(); }
  }
}
