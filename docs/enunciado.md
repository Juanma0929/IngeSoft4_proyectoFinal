Arquitectura de Software: Estimación de Velocidad Promedio por Ruta por Mes con Datos del
SITM-MIO – Proyecto Final
El Sistema Integrado de Transporte Masivo de Occidente (SITM-MIO) es el sistema de transporte
masivo de la ciudad de Cali, diseñado para movilizar de manera eficiente a cientos de miles de
usuarios diariamente a través de una red estructurada de rutas y estaciones. Su operación es
supervisada por el Centro de Control de Operación (CCO) de Metrocali, entidad encargada de
coordinar, gestionar y garantizar el cumplimiento del Plan de Servicios de Operación (PSO) por
parte de los concesionarios de los buses, así como de monitorear en tiempo real el funcionamiento
de estos y las condiciones del servicio en toda la ciudad.
El SITM-MIO opera con una infraestructura compuesta por aproximadamente 1300 buses (con
proyección de crecimiento hasta 2000 en los próximos dos años), que atienden cerca de 100 rutas
principales y movilizan alrededor de 450.000 pasajeros diariamente. Cada uno de estos buses está
equipado con cerca de 30 sensores conectados a un computador embebido, el cual se encarga de
recolectar información operativa, agruparlos en un registro denominado datagrama, conteniendo
información clave como la ubicación GPS, el estado de las puertas y otros indicadores relevantes
del vehículo. Este computador transmite los datagramas cada 20 o 30 segundos mediante protocolo
GPRS hacia un centro de datos.
En cuanto a su operación, el sistema del CCO debe ser capaz de gestionar tanto eventos rutinarios
como situaciones excepcionales. Entre estos eventos se encuentran actualizaciones de posición o
estado del bus, mientras que también se deben atender eventos críticos como fallas mecánicas,
accidentes, congestión vial o incidentes de seguridad tales como ingreso no autorizado de pasajeros
o atraco en progreso, algunos de los cuales pueden ser reportados directamente por el conductor
mediante una interfaz especial en el vehículo. Cada uno de estos eventos se codifica en un
datagrama, mediante un código para cada tipo de evento. Al Centro de Control de Operación llegan
diariamente entre 2,5 y 3 millones de eventos, lo que implica la necesidad de contar con
mecanismos eficientes de procesamiento, almacenamiento y análisis de grandes volúmenes de
datos, tanto en tiempo real como de forma histórica.
El objetivo principal del sistema a desarrollar es maximizar la confiabilidad y eficiencia en el
aseguramiento del cumplimiento del plan operativo por parte de los buses, y minimizar los costos
operativos, aprovechando la información recolectada para generar valor a partir de los datos
recopilados de la operación, tras años de funcionamiento. Sin embargo, para este proyecto inicial, la
organización quiere iniciar con un piloto con los datos de un año, por ejemplo, para brindar un
servicio a la comunidad que indique la velocidad promedio de desplazamiento por ruta, dado que
una de las quejas más frecuentes de los usuarios del SITM-MIO es que es poco confiable en los
tiempos de llegada de los buses de una ruta a los paraderos o estaciones de la misma


Su misión es diseñar la arquitectura del subsistema que involucre todos los elementos necesarios
para automatizar el cálculo de la velocidad promedio por ruta por mes con un subconjunto de datos
"piloto". Para lo anterior, cuenta con los siguientes archivos (en /opt/sitm-mio de x104m03, x205m03
y x206m03):
• lines-241-ActiveGT.csv: listado de todas las rutas activas en el subconjunto de datos.
• datagrams-MiniPilot.csv: subconjunto de datos de recorridos de buses para probar la
solución, a pequeña escala. La solución debe entregar soluciones completas y correctas.
• datagrams4Pilot.csv: similar al anterior, con nueve veces más datos que la versión mini.
• Diccionario_De_Datos-OkGTM.pdf: explicativo de las columnas de los dos archivos
anteriores.
Para todas las rutas activas del piloto (lines-241-ActiveGT.csv):
• Calcular las velocidades promedio por ruta por mes, inicialmente implementando una
solución monolítica usando los datos que están en /opt/sitm-mio/datagrams-MiniPilot.csv
• Implementar una versión 2 con un ThreadPool
• Determinar el punto a partir del cual vale la pena distribuir la solución
• Implementar una versión 3 con una solución distribuida usando patrones de diseño y en
particular al menos un patrón de diseño de distribución.
Para lo anterior, deben desarrollar y entregar:
A. (10%) Identificación y priorización de drivers de arquitectura y atributos de calidad. Atributo de
calidad funcional: correctitud (no requiere escenario de QAW). Atributo de calidad de QAW a
priorizar: performance(tiempo de respuesta) y escalabilidad.
B. (20%) Selección y justificación de patrones y estilos arquitectónicos.
C. (40%) Diseño global e integración de la arquitectura propuesta: diagrama de deployment
(diseño arquitectónico completo y consistente con las respuestas anteriores) que soporte el
escenario de QAW señalado. Debe explicitar las estructuras distribuidas de procesamiento y de
almacenamiento, y resaltar sobre el diagrama de deployment los patrones de diseño
introducidos para satisfacer el driver de arquitectura de performance y escalabilidad.
D. (30%) Implementación, despliegue y validación experimental de la arquitectura: la validación
experimental debe mostrar los tiempos de ejecución tanto de la versión monolítica como de la
distribuida, con distintas cantidades de nodos de procesamiento.
E. (10% Bono) Diseño e implementación de la visualización de recorrido de buses.
Notas:
1. Para el código fuente deben indicar el link del repositorio en github. Para la salida del cálculo
   de la velocidad promedio, pueden implementar un CLI o GUI para consultar las velocidades
   según la ruta y el mes, o generar un CSV con una matriz de rutas vs. meses, con
   encabezados claramente identificados.
2. Para la calificación, ver rúbrica adjunta.