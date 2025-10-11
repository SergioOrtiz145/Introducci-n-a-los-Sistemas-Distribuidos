/**
 * ============================================================
 * Título: ClientePS
 * Autores: Ana Sofia Grass, Sergio Ortiz, Isabella Palacio, Sebastián Vargas
 * Fecha: 2025-10-10
 * ============================================================
 * ClientePS lee solicitudes de un archivo de texto y las envía al servidor usando un socket ZeroMQ REQ.
 * Recibe y muestra la respuesta del servidor para cada solicitud.
 */
package com.proyecto;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.zeromq.SocketType;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;

public class ClientePS 
{
    /**
     * Lee un archivo de texto y retorna una lista con las solicitudes.
     * Si el archivo tiene menos de 20 líneas válidas, retorna una lista vacía.
     */
    public static List<String> leerArchivo(String archivo){
        List<String> solicitudes = new ArrayList<>();
        try(FileReader fr = new FileReader(archivo)){
            BufferedReader br = new BufferedReader(fr);
            String linea;
            // Lee cada línea del archivo y la agrega a la lista si no está vacía
            while((linea = br.readLine())!= null){
                linea = linea.trim();
                if(linea.isEmpty()){
                    continue;
                }
                solicitudes.add(linea);
            }
            // Verifica que haya al menos 20 solicitudes
            if(solicitudes.size() < 20){
                return new ArrayList<String>();
            }
            return solicitudes;

        }catch(Exception e){
            // Imprime cualquier excepción que ocurra al leer el archivo
            System.out.println(e.getMessage());
            return new ArrayList<>();
        }
    }

    public static void main( String[] args )
    {
        // Verifica que se haya pasado el nombre del archivo como argumento
        if(args.length < 1){
            System.out.println("Debes pasar el nombre de un archivo de texto con las solicitudes.");
            return;
        }
        String nombreArchivo = args[0];
        System.out.println("El nombre del archivo es: "+nombreArchivo);

        // Lee las solicitudes del archivo
        List<String> solicitudes = new ArrayList<>();
        solicitudes = leerArchivo(nombreArchivo);

        // Verifica que el archivo tenga al menos 20 solicitudes
        if(solicitudes.isEmpty()){
            System.out.println("El archivo de texto debe contener al menos 20 solicitudes");
            return;
        }

        // Crea el contexto y socket ZeroMQ para enviar solicitudes al servidor
        try(ZContext context = new ZContext()){
            // Se crea un socket para la conexión con el servidor
            Socket socket = context.createSocket(SocketType.REQ);
            socket.connect("tcp://localhost:5555");
            System.out.println("El cliente fue conectado correctamente al servidor.");
            // Envía cada solicitud y espera la respuesta
            for(String s : solicitudes){
                System.out.println("Se envia la solicitud:" + s);
                socket.send(s.getBytes(ZMQ.CHARSET), 0);
                String reply = socket.recvStr(0);
                System.out.println(reply);
            }
        }catch(Exception e){
            // Imprime cualquier excepción que ocurra durante la comunicación
            System.out.println(e.getMessage());
        }

    }
}
