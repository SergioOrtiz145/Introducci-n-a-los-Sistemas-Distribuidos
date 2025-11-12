# Taller de Sistemas Distribuidos – Hadoop HDFS

Presentado por:
- Daniela Medina
- Isabella Palacio
- Sergio Ortiz

---

## 1. Introducción

En este taller se implementó un sistema de archivos distribuidos utilizando Hadoop HDFS con tres computadores:  
- Uno configurado como **NameNode** (nodo maestro).  
- Dos configurados como **DataNodes** (nodos esclavos).

El **NameNode** cumple la función de controlador central del sistema, encargado de gestionar los metadatos y la ubicación de los archivos dentro del clúster, mientras que los **DataNodes** se encargan de almacenar físicamente los bloques de datos que componen dichos archivos.

El objetivo principal de la práctica fue comprender el funcionamiento de un entorno distribuido de almacenamiento, los mecanismos de replicación de datos y tolerancia a fallos, así como la interacción entre los nodos que conforman el sistema.

El proceso incluyó:  
- Instalación de Java y Hadoop.  
- Configuración de variables de entorno.  
- Edición de archivos de configuración (`core-site.xml`, `hdfs-site.xml`, `hadoop-env.sh`, `workers`).  
- Conexión SSH entre nodos.  
- Verificación mediante `start-dfs.sh` y la interfaz web del HDFS.

---

## 2. Configuraciones Generales del DataNode y NameNode
Cada uno de los siguientes comandos fueron ejecutados en cada computador que ejercia la función de DataNode y Namenode

#### - sudo apt update
Actualiza la lista de paquetes disponibles en los repositorios del sistema, para que se instalen las versiones mas recientes. 

<div align="center">
<img width="921" height="213" alt="image" src="https://github.com/user-attachments/assets/62486b19-2cc2-40b5-adfa-7aeb4dd9e43b" />
</div>

#### - sudo apt install openjdk-17-jdk -y 
Instala el JDK versión 17.0.16

<div align="center">
<img width="921" height="140" alt="image" src="https://github.com/user-attachments/assets/4de6cc34-ebfd-42cd-93fe-290aaf4653f9" />
</div>

#### - wget https://downloads.apache.org/hadoop/common/hadoop-3.3.6/hadoop-3.3.6.tar.gz 
Descarga el paquete comprimido de Hadoop, desde el repositorio oficial de Apache.

<div align="center">
<img width="921" height="333" alt="image" src="https://github.com/user-attachments/assets/86798174-904e-4acf-b962-cec609ba136a" />
</div>

#### - tar -xzvf hadoop-3.3.6.tar.gz 
Descomprime y extrae los archivos del paquete de Hadoop.

<div align="center">
<img width="921" height="430" alt="image" src="https://github.com/user-attachments/assets/970d7e5a-284d-4400-9afa-63e1b9a933da" />
</div>

#### - sudo mv hadoop-3.3.6 /usr/local/Hadoop
Mueve la carpeta de Hadoop a `/usr/local/`,

<div align="center">
<img width="921" height="35" alt="image" src="https://github.com/user-attachments/assets/45a54deb-ab60-4526-a2ca-4c9ec17d0ba5" />
</div>

#### - nano ~/.bashrc 
Se definen las variables de entorno. Indica la ruta en la que está instalada tanto Java como Hadoop. Además, permite que se ejecuten los comandos de Hadoop desde cualquier directorio.

<div align="center">
<img width="921" height="434" alt="image" src="https://github.com/user-attachments/assets/0319cc14-b16f-4953-92a5-12f4761e4742" />
</div>

#### - source ~/.bashr
Recarga el archivo, aplicando los cambios. 

<div align="center">
<img width="921" height="19" alt="image" src="https://github.com/user-attachments/assets/57c7b153-388b-478e-a1ce-d76bd2a8a9a4" />
</div>

#### - sudo nano core-side.xml
Se abre el archivo donde se define la configuración base de Hadoop, incluyendo la dirección del NameNode, que es el nodo maestro encargado de gestionar la estructura del sistema de archivos HDFS, en este caso su ip es 10.43.103.44 y 9000 es el puerto que usará Hadoop para las comunicaciones con el NameNode.

<div align="center">
<img width="921" height="426" alt="image" src="https://github.com/user-attachments/assets/13e21afa-488c-4e1d-b74a-4b59cc7a880d" />
</div>

#### - sudo nano hdfs-side.xml
Define la configuración específica del HDFS, tal como la replicación de datos y rutas locales de almacenamiento del NameNode.

<div align="center">
<img width="921" height="430" alt="image" src="https://github.com/user-attachments/assets/f8112f15-127e-42c8-a732-eb99ba519f16" />
</div>

#### - sudo nano /etc/hosts
Abre el archivo que asocia direcciones IP con nombres de host. Es muy importante para que Hadoop pueda comunicarse correctamente entre los nodos, es decir, tanto el NameNode como el DataNodes, usando nombres en lugar de direcciones IP.
En este caso la ip 10.43.103.44 representa al NameNode. Por otro lado, las ip 10.43.102.227 y 10.43.103.58 son DataNodes. Los otros datos son configuraciones predeterminadas para redes con compatibilidad IPv6

<div align="center">
<img width="921" height="431" alt="image" src="https://github.com/user-attachments/assets/460b6ee0-102a-4bd9-880d-6f256a53acb4" />
</div>

#### - sudo nano /usr/local/hadoop/etc/hadoop/hadoop-env.sh
Abre el archivo donde se configuran las variables de entorno que utiliza Hadoop en su ejecución interna. Le indica a Hadoop qué versión de Java usar y dónde se encuentran sus librerías.

<div align="center">
<img width="921" height="429" alt="image" src="https://github.com/user-attachments/assets/5e2c441c-788c-4cff-b89d-5b22c2d1ff87" />
</div>

#### - source /usr/local/hadoop/etc/hadoop/hadoop-env.sh
Recarga el archivo actualizado para que las variables definidas en él se apliquen a la sesión actual.

<div align="center">
<img width="921" height="19" alt="image" src="https://github.com/user-attachments/assets/588fba9a-04b3-4406-be57-60702854e6c7" />
</div>


## 3. Configuración Específica para el DataNode
El siguientes comando fue ejecutado en cada computador que ejercia la función de DataNode.

####	- mkdir -p /usr/local/hadoop/data/datanode
Crear el directorio donde el DataNode almacenará los datos que forman parte del sistema de HDFS.

<div align="center">
<img width="921" height="26" alt="image" src="https://github.com/user-attachments/assets/1303aa08-5df3-4394-859b-d262bc2b8d10" />
</div>

---

## 4. Configuración Específica del NameNode
Cada uno de los siguientes comandos fueron ejecutados en cada computador que ejercia la función de NameNode

#### - nano /usr/local/hadoop/etc/hadoop/workers
Se modifica el archivo workers para agregar los computadores esclavos (DataNodes) que forman parte del cluster de Hadoop.

<div align="center">
<img width="921" height="624" alt="image" src="https://github.com/user-attachments/assets/9ee62048-5ca9-440a-8eae-5caebe7dbeb4" />
</div>

#### - ssh-keygen -t rsa -P “”
Genera un par de llaves SSH para que el máster pueda conectarse a los esclavos sin necesidad de contraseña.

<div align="center">
<img width="921" height="538" alt="image" src="https://github.com/user-attachments/assets/1a5b1fa7-438d-411f-8f16-a0b01b7929e6" />
</div>

#### - ssh-copy-id estudiante@esclavo#
Copia la llave pública del máster a los esclavos para permitir conexión SSH sin contraseña.

<div align="center">
<img width="921" height="344" alt="image" src="https://github.com/user-attachments/assets/379e6330-0497-458b-9d55-fe49acfd14b7" />
</div>

#### - mkdir -p /usr/local/hadoop/data/namenode
Crea el directorio donde el NameNode guardará su metadata.

#### - hdfs namenode -format
Inicializa el sistema de archivos HDFS por primera vez.

<div align="center">
<img width="921" height="286" alt="image" src="https://github.com/user-attachments/assets/f0487536-6b5e-45b1-80c5-113c7bd83d1e" />
</div>

#### - start-dfs.sh
Inicia todos los servicios de HDFS en el clúster completo desde el master con un solo comando.

<div align="center">
<img width="622" height="117" alt="image" src="https://github.com/user-attachments/assets/f380d738-ebcc-4d09-894a-79dccfae11aa" />
</div>

#### - stop-dfs.sh
Detiene todos los servicios del HDFS en el clúster completo desde el master.

<div align="center">
<img width="622" height="117" alt="image" src="https://github.com/user-attachments/assets/88a8e3a5-5099-4975-a296-852a195f9085" />
</div>

---

## 5. Resumen del Cluster

<div align="center">
<img width="921" height="524" alt="image" src="https://github.com/user-attachments/assets/0c2195e6-a7b1-4246-b160-81cdcfeef814" />
</div>

<div align="center">
<img width="921" height="524" alt="image" src="https://github.com/user-attachments/assets/5662a84f-77ba-441b-acb5-16f25bca24ac" />
</div>

---

## 6. Análisis
Se observó que la comunicación entre nodos dependía directamente de la correcta configuración del archivo `/etc/hosts`, ya que cualquier error en las direcciones IP o en los nombres asignados podía impedir la conexión entre el NameNode y los DataNodes. Así mismo, la definición de las variables de entorno en `.bashrc` y `hadoop-env.sh` fue clave para que el sistema reconociera las rutas de Java y Hadoop y pudiera iniciar sin errores.
Una vez ejecutado el comando start-dfs.sh, el clúster mostró el funcionamiento esperado: los DataNodes se conectaron correctamente al NameNode, y fue posible visualizar el estado general del sistema a través de la interfaz web de Hadoop (puerto 9870).
Los valores de capacidad usada y disponible presentados en dicha interfaz confirmaron que el sistema estaba correctamente preparado para almacenar información y distribuir los bloques de datos de manera equilibrada entre ambos DataNodes.
Asimismo, el gráfico “DataNode Usage Histogram” evidenció actividad en los dos nodos, lo que validó que el sistema de archivos distribuido se encontraba operativo de forma correcta.
En conjunto, esto permitió confirmar la comunicación efectiva entre los equipos y comprobar que la distribución y gestión de los datos en el HDFS se realizaban de forma adecuada y estable.
Entre las principales dificultades encontradas estuvo la instalación de las versiones correctas de Java (17.0.6) y Hadoop (3.3.6), así como la sincronización de los entornos en los tres computadores. También fue necesario ajustar los permisos y rutas de los directorios donde se almacenaban los datos para evitar errores de acceso.
En general, el sistema mostró un comportamiento estable y coherente con los principios de los sistemas distribuidos: replicación de información, tolerancia a fallos y escalabilidad. El trabajo permitió comprender cómo HDFS divide y distribuye los datos en bloques entre distintos nodos, garantizando que el sistema siga operativo incluso si uno de los DataNodes deja de funcionar.

---

## 7. Conclusiones

La realización de este taller permitió comprender de manera práctica cómo funciona un sistema de archivos distribuido y la relevancia que tiene la coordinación entre múltiples nodos para el manejo eficiente de datos.

A través de la configuración del clúster Hadoop HDFS con un NameNode y dos DataNodes, se comprobó que la correcta definición de parámetros y rutas es esencial para lograr una comunicación estable y una distribución balanceada de la información.

Más allá del resultado técnico, la experiencia permitió fortalecer habilidades de resolución de problemas, trabajo colaborativo y comprensión de la arquitectura distribuida, Se comprobó que la arquitectura maestro–esclavo de Hadoop facilita la distribución y replicación de datos, mejorando la disponibilidad y la tolerancia a fallos

Finalmente, la práctica evidenció que Hadoop HDFS es una solución robusta y confiable para el almacenamiento masivo de datos, ofreciendo una base sólida para comprender los fundamentos de los sistemas distribuidos

---

## 8. Referencias

* Steps to Install and Configure HDFS | MOSIP Docs 1.1.5. (s. f.). MOSIP Docs 1.1.5. https://docs-mosip-io.translate.goog/1.1.5/build-and-deploy/other-installation-guides/steps-to-install-and-configure-hdfs?_x_tr_sl=en&_x_tr_tl=es&_x_tr_hl=es&_x_tr_pto=tc&_x_tr_hist=true
* Javier Abellán Ferrer. (2024, 5 febrero). Puesta en marcha del clúster HDFS de Hadoop [Vídeo]. YouTube. https://www.youtube.com/watch?v=cmGDpV30Daw
* Como auxiliar extra se utilizo la herramienta de Inteligencia Artificial ChatGPT.














