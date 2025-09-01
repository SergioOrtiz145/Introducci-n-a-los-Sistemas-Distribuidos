import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BibliotecaRemote extends Remote {
  String prestarPorISBN(String isbn) throws RemoteException;
  String prestarPorTitulo(String titulo) throws RemoteException;
  String consultarPorISBN(String isbn) throws RemoteException;
  String devolverPorISBN(String isbn) throws RemoteException;
}
