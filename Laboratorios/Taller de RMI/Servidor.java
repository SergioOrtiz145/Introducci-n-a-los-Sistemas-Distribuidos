import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Servidor {
  public static void main(String[] args) {
    try {
      int port = (args.length > 0) ? Integer.parseInt(args[0]) : 1099;

      // Levanta registry si no existe
      try { LocateRegistry.createRegistry(port); } catch (Exception ignored) {}

      BibliotecaRemote servicio = new BibliotecaRemoteImpl();
      Registry registry = LocateRegistry.getRegistry(port);

      String nombre = "BibliotecaService";
      registry.rebind(nombre, servicio);

      System.out.println("Servidor RMI listo en puerto " + port + " con nombre: " + nombre);
      System.out.println("Ctrl+C para finalizar.");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}