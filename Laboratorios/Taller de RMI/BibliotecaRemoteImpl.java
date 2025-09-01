import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

//Esta clase define el comportamiento real de los métodos del servicio remoto de la biblioteca.
public class BibliotecaRemoteImpl extends UnicastRemoteObject implements BibliotecaRemote {
  //Lista de libros disponibles en la biblioteca.
  private List<Libro> libros;

  //El constructor carga los libros desde el archivo al
  //iniciar el servidor.
  public BibliotecaRemoteImpl() throws RemoteException {
    libros = GestorArchivo.leerLibros();
  }

  //Calcula la fehca de devolución para un préstamo, que es una
  //semana desde la fecha actual.
  private static String fechaEnUnaSemana() {
    Calendar c = Calendar.getInstance();
    c.add(Calendar.DAY_OF_MONTH, 7);
    return new SimpleDateFormat("dd-MM-yyyy").format(c.getTime());
  }

  //Función para pedir un prestamo de un libro mediante un ISBN
  //Si el préstamo es exitoso, retorna la fecha de devolución.
  //Si no hay disponibles, imprime "No hay disponible".
  @Override
  public synchronized String prestarPorISBN(String isbn) throws RemoteException {
    for (Libro libro : libros) {
      if (libro.getIsbn().equals(isbn) && libro.getCantidad() > 0) {
        libro.setCantidad(libro.getCantidad() - 1);
        String fecha = fechaEnUnaSemana();
        GestorArchivo.guardarLibros(libros);
        return fecha;
      }
    }
    return "No disponible";
  }

  //Función para pedir un prestamo de un libro mediante un titulo
  //Si el préstamo es exitoso, retorna la fecha de devolución.
  //Si no hay disponibles, imprime "No hay disponible".
  @Override
  public synchronized String prestarPorTitulo(String titulo) throws RemoteException {
    for (Libro libro : libros) {
      if (libro.getTitulo().equalsIgnoreCase(titulo) && libro.getCantidad() > 0) {
        libro.setCantidad(libro.getCantidad() - 1);
        String fecha = fechaEnUnaSemana();
        GestorArchivo.guardarLibros(libros);
        return fecha;
      }
    }
    return "No disponible";
  }

  //Función para consultar un libro por un ISBN
  //Si el ISBN existe en el servidor, indica la disponibilidad del libro.
  //Si no existe, imprime "No existe".
  @Override
  public synchronized String consultarPorISBN(String isbn) throws RemoteException {
    for (Libro libro : libros) {
      if (libro.getIsbn().equals(isbn)) {
        return "ISBN " + isbn + ": disponibles=" + libro.getCantidad();
      }
    }
    return "No existe";
  }

  //Función para devolver un libro por un ISBN
  //Si el ISBN esta en el servidor, se aumenta en 1 la cantidad disponibles del libro
  //imprime "Devuelto".
  //Si el ISBN no esta en el servidor, imprime "No existe".
  @Override
  public synchronized String devolverPorISBN(String isbn) throws RemoteException {
    for (Libro libro : libros) {
      if (libro.getIsbn().equals(isbn)) {
        libro.setCantidad(libro.getCantidad() + 1);
        GestorArchivo.guardarLibros(libros);
        return "Devuelto";
      }
    }
    return "No existe";
  }
}