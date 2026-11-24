/*
 * Maqueta de cubículos – ESP32 conectado a la página de reservas.
 *
 * Por cada cubículo: un sensor PIR y un tramo de la tira LED (WS2812/NeoPixel). Un DFPlayer Mini para los sonidos.
 * Habla con el backend por HTTP usando la cabecera X-Sensor-Token:
 *   POST /api/sensores/{n}/presencia   cuando el PIR detecta a alguien
 *   GET  /api/sensores/{n}/estado      cada ~3 s, para saber qué luz mostrar (también cuenta como latido)
 *
 * Librerías (Gestor de librerías del Arduino IDE):
 *   - ArduinoJson (Benoit Blanchon) 7.x
 *   - Adafruit NeoPixel
 *   - DFRobotDFPlayerMini
 * Placa: "ESP32 Dev Module" (paquete esp32 de Espressif 2.x o 3.x).
 *
 * Antes de compilar: copia ejemplo_secrets.h como secrets.h y pon tu WiFi, la IP del servidor y el token.
 */

#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <Adafruit_NeoPixel.h>
#include <DFRobotDFPlayerMini.h>
#include "secrets.h"

// ------------------------------------------------------------------ Hardware (ajusta a tu maqueta)
const int NUM_CUBICULOS = 4;
const int NUMERO_CUBICULO[NUM_CUBICULOS] = {1, 2, 3, 4};    // número que tiene cada uno en la página
const int PIN_PIR[NUM_CUBICULOS]         = {32, 33, 34, 35}; // salida OUT de cada PIR

const int PIN_TIRA_LED       = 5;   // DIN de la tira
const int LEDS_POR_CUBICULO  = 8;   // la tira se reparte en tramos seguidos: cubículo 1, 2, 3, 4
const uint8_t BRILLO         = 80;  // 0-255 (con USB no pases de ~80 si son muchos LEDs)

const int PIN_DFPLAYER_RX = 16;     // RX del ESP32  <- TX del DFPlayer
const int PIN_DFPLAYER_TX = 17;     // TX del ESP32  -> RX del DFPlayer (con resistencia de 1 kΩ)
const int VOLUMEN         = 22;     // 0-30

// Pistas en la microSD: carpeta /mp3 con 0001.mp3, 0002.mp3, ...
const int PISTA_BIENVENIDA_INDIVIDUAL = 1;
const int PISTA_BIENVENIDA_GRUPO      = 2;
const int PISTA_AVISO_5_MIN           = 3;
const int PISTA_DESPEDIDA             = 4;

// ------------------------------------------------------------------ Tiempos
const unsigned long CALENTAMIENTO_PIR_MS  = 45000; // el PIR da falsos positivos al encender
const unsigned long CICLO_ESTADO_MS       = 3000;  // cada cubículo pregunta su estado cada 3 s
const unsigned long REPETIR_PRESENCIA_MS  = 5000;  // mientras el PIR siga activo, reportar como máximo cada 5 s
const unsigned long TIMEOUT_HTTP_MS       = 1500;
const int           FALLOS_PARA_SIN_RED   = 3;     // tras 3 fallos seguidos se pasa al modo sin conexión
const unsigned long LUZ_SIN_RED_MS        = 60000; // sin servidor: PIR -> luz azul durante 1 min
const unsigned long REINTENTO_WIFI_MS     = 10000;

// ------------------------------------------------------------------ Estado
struct Cubiculo {
  String luz = "APAGADA";        // APAGADA | AMBAR | AZUL | ROJO | VIOLETA
  String estado = "";            // LIBRE | RESERVADO | OCUPADO | POR_TERMINAR | OCUPADO_ADMIN | MANTENIMIENTO | CERRADO
  int fallos = 0;
  bool pirAnterior = false;
  unsigned long ultimaPresenciaEnviada = 0;
  unsigned long ultimoMovimiento = 0;
  bool huboMovimiento = false;
};

Cubiculo cubiculos[NUM_CUBICULOS];
Adafruit_NeoPixel tira(NUM_CUBICULOS * LEDS_POR_CUBICULO, PIN_TIRA_LED, NEO_GRB + NEO_KHZ800);
HardwareSerial serialDf(2);
DFRobotDFPlayerMini dfplayer;
bool hayDfplayer = false;

unsigned long inicio = 0;
unsigned long ultimaConsulta = 0;
unsigned long ultimoIntentoWifi = 0;
int siguiente = 0; // a qué cubículo le toca consultar (se reparte para no bloquear el loop)

// ------------------------------------------------------------------ Luces y sonido
uint32_t colorDe(const String& luz) {
  if (luz == "AMBAR")   return tira.Color(255, 120, 0);
  if (luz == "AZUL")    return tira.Color(0, 60, 255);
  if (luz == "ROJO")    return tira.Color(255, 0, 0);
  if (luz == "VIOLETA") return tira.Color(150, 0, 255);
  return 0; // APAGADA
}

void pintar(int i, uint32_t color) {
  for (int k = 0; k < LEDS_POR_CUBICULO; k++) {
    tira.setPixelColor(i * LEDS_POR_CUBICULO + k, color);
  }
}

void reproducir(int pista) {
  Serial.printf("  >> sonido: pista %d\n", pista);
  if (hayDfplayer) dfplayer.play(pista);
}

bool sinRed(int i) {
  return WiFi.status() != WL_CONNECTED || cubiculos[i].fallos >= FALLOS_PARA_SIN_RED;
}

void actualizarTira() {
  unsigned long ahora = millis();
  for (int i = 0; i < NUM_CUBICULOS; i++) {
    Cubiculo& c = cubiculos[i];
    if (sinRed(i)) {
      // Igual que el sketch original: si hay movimiento, luz azul un rato.
      bool reciente = c.huboMovimiento && ahora - c.ultimoMovimiento < LUZ_SIN_RED_MS;
      pintar(i, reciente ? colorDe("AZUL") : 0);
    } else {
      pintar(i, colorDe(c.luz));
    }
  }
  tira.show();
}

// ------------------------------------------------------------------ Servidor
bool esOcupado(const String& estado) {
  return estado == "OCUPADO" || estado == "POR_TERMINAR";
}

/** Aplica la respuesta del servidor: guarda la luz y decide qué sonido toca por el cambio de estado. */
void aplicarRespuesta(int i, const String& cuerpo) {
  JsonDocument doc;
  DeserializationError err = deserializeJson(doc, cuerpo);
  if (err) {
    Serial.printf("Cubiculo %d: JSON invalido (%s)\n", NUMERO_CUBICULO[i], err.c_str());
    return;
  }
  Cubiculo& c = cubiculos[i];
  String luz = doc["luz"] | "APAGADA";
  String estado = doc["estado"] | "";
  String tipo = doc["tipoReserva"] | "";
  bool checkIn = doc["checkInConfirmado"] | false;
  bool conocido = c.estado.length() > 0; // justo al arrancar no se reproduce nada

  if (checkIn) {
    reproducir(tipo == "GRUPO" ? PISTA_BIENVENIDA_GRUPO : PISTA_BIENVENIDA_INDIVIDUAL);
  } else if (conocido && luz == "ROJO" && c.luz != "ROJO") {
    reproducir(PISTA_AVISO_5_MIN);
  } else if (conocido && esOcupado(c.estado) && !esOcupado(estado)) {
    reproducir(PISTA_DESPEDIDA);
  }

  if (estado != c.estado || luz != c.luz) {
    Serial.printf("Cubiculo %d: %s / luz %s\n", NUMERO_CUBICULO[i], estado.c_str(), luz.c_str());
  }
  c.luz = luz;
  c.estado = estado;
}

/** Llama a /api/sensores/{n}/{accion}. Devuelve true si el servidor respondió 200. */
bool llamar(int i, const char* metodo, const char* accion) {
  if (WiFi.status() != WL_CONNECTED) return false;
  String url = String(SERVIDOR) + "/api/sensores/" + NUMERO_CUBICULO[i] + "/" + accion;
  HTTPClient http;
  http.setConnectTimeout(TIMEOUT_HTTP_MS);
  http.setTimeout(TIMEOUT_HTTP_MS);
  if (!http.begin(url)) return false;
  http.addHeader("X-Sensor-Token", SENSOR_TOKEN);
  int codigo = strcmp(metodo, "POST") == 0 ? http.POST("") : http.GET();
  bool ok = codigo == 200;
  if (ok) {
    cubiculos[i].fallos = 0;
    aplicarRespuesta(i, http.getString());
  } else {
    cubiculos[i].fallos++;
    if (codigo == 401 || codigo == 403) {
      Serial.printf("Cubiculo %d: el servidor rechazo el token (HTTP %d). Revisa SENSOR_TOKEN.\n", NUMERO_CUBICULO[i], codigo);
    } else if (codigo == 404) {
      Serial.printf("Cubiculo %d: no existe en el servidor (HTTP 404).\n", NUMERO_CUBICULO[i]);
    } else {
      Serial.printf("Cubiculo %d: %s %s fallo (%d %s)\n", NUMERO_CUBICULO[i], metodo, accion, codigo,
                    http.errorToString(codigo).c_str());
    }
  }
  http.end();
  return ok;
}

// ------------------------------------------------------------------ PIR
void leerSensores() {
  unsigned long ahora = millis();
  bool calentando = ahora - inicio < CALENTAMIENTO_PIR_MS;
  for (int i = 0; i < NUM_CUBICULOS; i++) {
    Cubiculo& c = cubiculos[i];
    bool pir = digitalRead(PIN_PIR[i]) == HIGH;
    if (pir && !calentando) {
      c.ultimoMovimiento = ahora;
      c.huboMovimiento = true;
      bool flanco = !c.pirAnterior;
      if (flanco || ahora - c.ultimaPresenciaEnviada >= REPETIR_PRESENCIA_MS) {
        c.ultimaPresenciaEnviada = ahora;
        Serial.printf("Cubiculo %d: movimiento\n", NUMERO_CUBICULO[i]);
        llamar(i, "POST", "presencia");
      }
    }
    c.pirAnterior = pir;
  }
}

// ------------------------------------------------------------------ WiFi
void conectarWifi() {
  WiFi.mode(WIFI_STA);
  WiFi.setAutoReconnect(true);
  WiFi.begin(WIFI_SSID, WIFI_CLAVE);
  Serial.printf("Conectando a %s", WIFI_SSID);
  unsigned long t = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - t < 15000) {
    delay(300);
    Serial.print(".");
  }
  if (WiFi.status() == WL_CONNECTED) {
    Serial.printf("\nWiFi listo. IP de la maqueta: %s  Servidor: %s\n", WiFi.localIP().toString().c_str(), SERVIDOR);
  } else {
    Serial.println("\nSin WiFi: la maqueta funciona sola (PIR -> luz azul) y reintenta en segundo plano.");
  }
  ultimoIntentoWifi = millis();
}

void vigilarWifi() {
  if (WiFi.status() == WL_CONNECTED) return;
  if (millis() - ultimoIntentoWifi >= REINTENTO_WIFI_MS) {
    ultimoIntentoWifi = millis();
    Serial.println("Reintentando WiFi...");
    WiFi.disconnect();
    WiFi.begin(WIFI_SSID, WIFI_CLAVE);
  }
}

// ------------------------------------------------------------------ Arduino
void setup() {
  Serial.begin(115200);
  delay(200);
  Serial.println("\n== Maqueta de cubiculos ==");

  for (int i = 0; i < NUM_CUBICULOS; i++) pinMode(PIN_PIR[i], INPUT);

  tira.begin();
  tira.setBrightness(BRILLO);
  tira.fill(tira.Color(40, 40, 40)); // blanco tenue mientras arranca
  tira.show();

  serialDf.begin(9600, SERIAL_8N1, PIN_DFPLAYER_RX, PIN_DFPLAYER_TX);
  hayDfplayer = dfplayer.begin(serialDf);
  if (hayDfplayer) {
    dfplayer.volume(VOLUMEN);
    Serial.println("DFPlayer listo.");
  } else {
    Serial.println("DFPlayer no responde (cableado o microSD?). Sigo sin sonido.");
  }

  conectarWifi();
  inicio = millis();
  Serial.printf("Calentando los PIR %lu s...\n", CALENTAMIENTO_PIR_MS / 1000);
  tira.clear();
  tira.show();
}

void loop() {
  vigilarWifi();
  leerSensores();

  // Se consulta un cubículo a la vez, repartidos en el ciclo, para que el loop siga atento al PIR.
  unsigned long paso = CICLO_ESTADO_MS / NUM_CUBICULOS;
  if (millis() - ultimaConsulta >= paso) {
    ultimaConsulta = millis();
    llamar(siguiente, "GET", "estado");
    siguiente = (siguiente + 1) % NUM_CUBICULOS;
  }

  actualizarTira();
  delay(20);
}
