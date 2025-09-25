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
    public static List<String> leerArchivo(String archivo){
        List<String> solicitudes = new ArrayList<>();
        try(FileReader fr = new FileReader(archivo)){
            BufferedReader br = new BufferedReader(fr);
            String linea;
            while((linea = br.readLine())!= null){
                linea = linea.trim();
                if(linea.isEmpty()){
                    continue;
                }
                solicitudes.add(linea);
            }
            if(solicitudes.size() < 20){
                return new ArrayList<String>();
            }
            return solicitudes;

        }catch(Exception e){
            System.out.println(e.getMessage());
            return new ArrayList<>();
        }
    }
    public static void main( String[] args )
    {
        if(args.length < 1){
            System.out.println("Debes pasar el nombre de un archivo de texto con las solicitudes.");
            return;
        }
        String nombreArchivo = args[0];
        System.out.println("El nombre del archivo es: "+nombreArchivo);
        List<String> solicitudes = new ArrayList<>();
        solicitudes = leerArchivo(nombreArchivo);
        if(solicitudes.isEmpty()){
            System.out.println("El archivo de texto debe contener al menos 20 solicitudes");
            return;
        }
        try(ZContext context = new ZContext()){
            //Se crea un socket para la conexión con el servidor
            Socket socket = context.createSocket(SocketType.REQ);
            socket.connect("tcp://localhost:5555");
            System.out.println("El cliente fue conectado correctamente al servidor.");
            for(String s : solicitudes){
                System.out.println("Se envia la solicitud:" + s);
                socket.send(s.getBytes(ZMQ.CHARSET), 0);
                String reply = socket.recvStr(0);
                System.out.println(reply);
            }
        }catch(Exception e){
            System.out.println(e.getMessage());
        }

    }
}
