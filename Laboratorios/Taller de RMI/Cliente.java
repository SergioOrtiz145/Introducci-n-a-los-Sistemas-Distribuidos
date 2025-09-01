import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Cliente {
  public static void main(String[] args) {
    String host = (args.length > 0) ? args[0] : "localhost";
    int port    = (args.length > 1) ? Integer.parseInt(args[1]) : 1099;

    try {
      Registry registry = LocateRegistry.getRegistry(host, port);
      BibliotecaRemote api = (BibliotecaRemote) registry.lookup("BibliotecaService");

      // PRUEBA DEMO
      System.out.println(api.consultarPorISBN("978-0134685991"));
      System.out.println(api.prestarPorISBN("978-0134685991"));
      System.out.println(api.consultarPorISBN("978-0134685991"));
      System.out.println(api.devolverPorISBN("978-0134685991"));
      System.out.println(api.prestarPorTitulo("Head First Java"));

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}