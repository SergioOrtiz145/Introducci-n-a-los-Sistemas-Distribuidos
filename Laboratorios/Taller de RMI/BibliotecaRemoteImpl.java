import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class BibliotecaRemoteImpl extends UnicastRemoteObject implements BibliotecaRemote {

  private List<Libro> libros;

  public BibliotecaRemoteImpl() throws RemoteException {
    libros = GestorArchivo.leerLibros();
  }

  private static String fechaEnUnaSemana() {
    Calendar c = Calendar.getInstance();
    c.add(Calendar.DAY_OF_MONTH, 7);
    return new SimpleDateFormat("dd-MM-yyyy").format(c.getTime());
  }

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

  @Override
  public synchronized String consultarPorISBN(String isbn) throws RemoteException {
    for (Libro libro : libros) {
      if (libro.getIsbn().equals(isbn)) {
        return "ISBN " + isbn + ": disponibles=" + libro.getCantidad();
      }
    }
    return "No existe";
  }

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