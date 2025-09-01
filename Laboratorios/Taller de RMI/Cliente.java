import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

//Clase de cliente RMI para el servicio de Biblioteca
//Permite a un usuario interactuar con el servidor remoto
//a través de un menú en consola.
public class Cliente {
  public static void main(String[] args) {
    //Host y puerto del servidor
    String host = (args.length > 0) ? args[0] : "localhost";
    int port    = (args.length > 1) ? Integer.parseInt(args[1]) : 1099;

    try {
      //Obtener referencia al registro RMI
      Registry registry = LocateRegistry.getRegistry(host, port);
      // Buscar el servicio remoto por su nombre
      BibliotecaRemote api = (BibliotecaRemote) registry.lookup("BibliotecaService");

      // Scanner para leer las opciones del usuario
      Scanner sc = new Scanner(System.in);
      int opcion;
      // Bucle del menú
      do {
        System.out.println("\n===== MENÚ BIBLIOTECA =====");
        System.out.println("1. Consultar libro por ISBN");
        System.out.println("2. Prestar libro por ISBN");
        System.out.println("3. Prestar libro por Título");
        System.out.println("4. Devolver libro por ISBN");
        System.out.println("5. Salir");
        System.out.print("Elija una opción: ");

        // Leer opción elegida por el usuario
        opcion = sc.nextInt();
        sc.nextLine(); // limpiar buffer

        //Ejecutar acción según la opción elegida
        switch (opcion) {
          case 1:
            System.out.print("Ingrese ISBN: ");
            String isbnC = sc.nextLine();
            System.out.println(api.consultarPorISBN(isbnC));
            break;
          case 2:
            System.out.print("Ingrese ISBN: ");
            String isbnP = sc.nextLine();
            System.out.println("Resultado: " + api.prestarPorISBN(isbnP));
            break;
          case 3:
            System.out.print("Ingrese Título: ");
            String titulo = sc.nextLine();
            System.out.println("Resultado: " + api.prestarPorTitulo(titulo));
            break;
          case 4:
            System.out.print("Ingrese ISBN: ");
            String isbnD = sc.nextLine();
            System.out.println("Resultado: " + api.devolverPorISBN(isbnD));
            break;
          case 5:
            System.out.println("Saliendo del cliente...");
            break;
          default:
            System.out.println("Opción no válida.");
        }
      } while (opcion != 5);

      sc.close();

    } catch (Exception e) {
      //En caso de error en la comunicación con el servidor remoto.
      e.printStackTrace();
    }
  }
}
