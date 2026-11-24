// Copia este archivo como secrets.h (en esta misma carpeta) y completa tus datos.
// secrets.h NO se sube a git (.gitignore ignora secrets.*).
#pragma once

#define WIFI_SSID     "NombreDeTuRed"     // el ESP32 solo usa WiFi de 2.4 GHz
#define WIFI_CLAVE    "ClaveDeTuRed"

// IP de la PC donde corre el backend (ipconfig -> "Dirección IPv4"), en la MISMA red que el ESP32.
// Sin barra al final. En desarrollo el puerto es 8095.
#define SERVIDOR      "http://192.168.1.50:8095"

// Igual a reservas.sensor-token del backend: en desarrollo está en application-dev.yml;
// en producción es la variable de entorno RESERVAS_SENSOR_TOKEN.
#define SENSOR_TOKEN  "pon-aqui-el-token-del-sensor"
