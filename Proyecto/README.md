# Proyecto Sistema Distribuido de Gestión de Bibliotecas
Se presenta un sistema distribuido que gestiona préstamos de libros en múltiples sedes con las siguientes características:

* Arquitectura distribuida: Ideal para 2 sedes independientes.
* Replicación bidireccional: Sincronización automática entre sedes.
* Tolerancia a fallos: Failover automático entre sedes.
* Comunicación síncrona: Patrón Req/Rep para préstamos.
* Comunicación asíncrona: Pátron Pub/Sub para renovaciones y devoluciones.
* Persistencia: Almacenamiento en archivos de texto.

## Requisitos previos para su ejecución
* Java: JDK 11 o superior.
* Maven: 3.6 o superior.
* Memoria: 2 GB RAM mínimo.

## Configuración inicial del proyecto
1. Clonar el proyecto
```
git clone https://github.com/SergioOrtiz145/Introducci-n-a-los-Sistemas-Distribuidos/tree/dev
```
2. Compilar el proyecto
```
mvn clean compile
```
3. Generar los datos iniciales (Libros y préstamos de cada sede)
```
mvn exec:java -Dexec.mainClass=com.proyecto.Testing.GeneradorDatosIniciales -Dexec.args="./datos"
```

## Ejecución del sistema
A continuación se va a enlistar cada componente a ejecutar en diferentes terminales ordenadas para que funcione correctamente una sede, este proceso
se debe realizar por cada sede a ejecutar.

1. GA
```
mvn "exec:java" \\
"-Dexec.mainClass=com.proyecto.Gestores.GA "\\
"-Dexec.args="SEDE1 ./datos/sede1 5555 5655 tcp://direccionIPSedeRemota:6655 true"
```
* 5555 es el puerto donde va a recibir las solicitudes de los actores.
* 5655 es el puerto para la replicación entre los GAs.
* tcp://direccionIpSedeRemota:6655 es la dirección IP y el puerto de comunicación con la otra sede.
* true se define si es o no el GA de la sede 1.

2. ActorPrestamo
```
mvn exec:java \\
-Dexec.mainClass=com.proyecto.Actores.ActorPrestamo \\
-Dexec.args="tcp://direccionIPSedeLocal:5555 tcp://direccionIPSedeRemota:6555"
```
* tcp://direccionIpSedeLocal:5555 es la dirección IP y el puerto de comunicación con el GA local.
* tcp://direccionIpSedeRemota:6555 es la dirección IP y el puerto de comunicación con el GA Remoto.

3. ActorDevolver
```
mvn exec:java \\
-Dexec.mainClass=com.proyecto.Actores.ActorDevolver \\
-Dexec.args="tcp://direccionIPSedeLocal:5555 tcp://direccionIPSedeRemota:6555"
```
* tcp://direccionIpSedeLocal:5555 es la dirección IP y el puerto de comunicación con el GA local.
* tcp://direccionIpSedeRemota:6555 es la dirección IP y el puerto de comunicación con el GA Remoto.
* Opcional: si desea probar el actor usando comunicación sincrona, solo se necesita cambiar esto:
```
-Dexec.mainClass=com.proyecto.Actores.ActorDevolver
```
por esto:
```
-Dexec.mainClass=com.proyecto.Actores.ActorDevolverSincrono
```
4. ActorRenovar
```
mvn exec:java \\
 -Dexec.mainClass=com.proyecto.Actores.ActorRenovar \\
 -Dexec.args="tcp://direccionIPSedeLocal:5555 tcp://direccionIPSedeRemota:6555"
```
* tcp://direccionIpSedeLocal:5555 es la dirección IP y el puerto de comunicación con el GA local.
* tcp://direccionIpSedeRemota:6555 es la dirección IP y el puerto de comunicación con el GA Remoto.
* Opcional: si desea probar el actor usando comunicación sincrona, solo se necesita cambiar esto:
```
-Dexec.mainClass=com.proyecto.Actores.ActorRenovar
```
por esto:
```
-Dexec.mainClass=com.proyecto.Actores.ActorRenovarSincrono
```

5. GC
```
mvn exec:java -Dexec.mainClass=com.proyecto.Gestores.GC 
```
6. ClientePS
```
mvn exec:java \\
-Dexec.mainClass=com.proyecto.ClientePS \\
-Dexec.args="cliente\_sede1 ./solicitudes.txt tcp://direccionIPSede:5565"
```
* cliente\_sede1 es el nombre del Cliente
* ./solicitudes.txt es el archivo de texto que contiene las solicitudes de préstamo, renovación y devolución. (mínimo 20 operaciones)
* tcp://direccionIPSede:5565 es la dirección IP de la sede a enviar las solicitudes y el puerto que usa el GC para la comunicación con el PS.

## Autores
* Sergio Ortiz
* Isabella Palacio
* Juan Sebastián Vargas
* Ana Sofía Grass
