import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

//Clase que levanta el servidor RMI de la biblioteca.
public class Servidor {
  public static void main(String[] args) {
    try {
      // Determinar el puerto (por defecto 1099 si no se pasa argumento)
      int port = (args.length > 0) ? Integer.parseInt(args[0]) : 1099;

      // Levanta registry si no existe
      try { LocateRegistry.createRegistry(port); } catch (Exception ignored) {}
      //Instanciar el servicio remoto
      BibliotecaRemote servicio = new BibliotecaRemoteImpl();
      //Obtener el registro RMI
      Registry registry = LocateRegistry.getRegistry(port);
      //Registrar el servicio bajo un nombre 
      String nombre = "BibliotecaService";
      registry.rebind(nombre, servicio);

      //Mensaje de confirmación
      System.out.println("Servidor RMI listo en puerto " + port + " con nombre: " + nombre);
      System.out.println("Ctrl+C para finalizar.");
    } catch (Exception e) {
      // Manejo de cualquier error referente a conexión entre otros.
      e.printStackTrace();
    }
  }
}