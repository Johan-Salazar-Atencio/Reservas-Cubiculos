# Firmware de la maqueta (ESP32)

Conecta la maqueta con la página: el PIR de cada cubículo confirma la llegada y la tira LED muestra el color que decide el servidor.

```
 PIR ──► ESP32 ──POST /api/sensores/{n}/presencia──► backend (Spring Boot)
         ESP32 ◄──GET  /api/sensores/{n}/estado──── { luz, estado, tipoReserva, checkInConfirmado, ... }
           │
           ├─► tira LED (color de "luz")
           └─► DFPlayer (bienvenida / aviso 5 min / despedida)
```

## 1. Cableado (valores por defecto, se cambian al inicio del `.ino`)

| Pieza | Pin de la pieza | ESP32 |
|---|---|---|
| PIR cubículo 1 / 2 / 3 / 4 | OUT | GPIO 32 / 33 / 34 / 35 |
| PIR (todos) | VCC · GND | 5V (VIN) · GND |
| Tira WS2812 | DIN (con resistencia de 330 Ω) | GPIO 5 |
| Tira WS2812 | 5V · GND | fuente de 5 V · GND común con el ESP32 |
| DFPlayer Mini | TX | GPIO 16 (RX2) |
| DFPlayer Mini | RX (con resistencia de 1 kΩ) | GPIO 17 (TX2) |
| DFPlayer Mini | VCC · GND · SPK1/SPK2 | 5V · GND · parlante |

La tira se reparte en tramos de `LEDS_POR_CUBICULO` LEDs (8 por defecto): primero el cubículo 1, luego el 2, etc.
Si tu maqueta tiene un solo cubículo, deja `NUM_CUBICULOS = 1` y los arreglos con un solo valor.

**microSD del DFPlayer** (FAT32): carpeta `mp3/` con
`0001.mp3` bienvenida individual · `0002.mp3` bienvenida de grupo · `0003.mp3` aviso «quedan 5 minutos» · `0004.mp3` despedida.

## 2. Preparar el servidor

1. Arranca el backend en modo desarrollo (ver el README principal). Escucha en el puerto **8095** de tu PC.
2. Busca la IP de tu PC en la red WiFi: `ipconfig` → «Dirección IPv4» (ej. `192.168.1.50`). La PC y el ESP32 deben estar **en la misma red**.
3. Abre el puerto en el firewall de Windows (PowerShell **como administrador**, una sola vez):
   ```powershell
   New-NetFirewallRule -DisplayName "Reservas API 8095" -Direction Inbound -Protocol TCP -LocalPort 8095 -Action Allow -Profile Private
   ```
   Si tu red figura como «Pública» en Windows, cámbiala a «Privada» o usa `-Profile Any`.
4. Prueba desde otra máquina o el celular (misma red): `http://<IP>:8095/swagger-ui.html` debe abrir.

## 3. Cargar el firmware

1. Arduino IDE → Placas: instala **esp32 by Espressif**; elige **ESP32 Dev Module**.
2. Gestor de librerías: **ArduinoJson** (7.x), **Adafruit NeoPixel**, **DFRobotDFPlayerMini**.
3. En `maqueta_esp32/`, copia `ejemplo_secrets.h` como `secrets.h` y completa: WiFi (2.4 GHz), `SERVIDOR` = `http://<IP>:8095`
   y `SENSOR_TOKEN` = el `sensor-token` de `application-dev.yml` (en producción, la variable `RESERVAS_SENSOR_TOKEN`).
4. Sube el sketch y abre el Monitor Serie a **115200**. Debe decir `WiFi listo` y luego mostrar el estado de cada cubículo.

## 4. Probar de punta a punta

1. En la página, entra como estudiante DEMO y reserva el turno actual de un cubículo → su luz pasa a **ámbar**.
2. Espera a que terminen los 45 s de calentamiento del PIR y pasa la mano → el servidor confirma la llegada:
   luz **azul**, suena la bienvenida y en la página el cubículo pasa a «Ocupado».
3. En **Administrador → Simulador** adelanta el reloj: a falta de 5 min la luz pasa a **roja** (aviso); al terminar se apaga (despedida).
4. Prueba «Mantenimiento» desde el administrador → luz **violeta**.

Para verificar sin la placa, la misma llamada con curl:
```bash
curl -X POST http://<IP>:8095/api/sensores/1/presencia -H "X-Sensor-Token: <token>"
```

## Problemas comunes

| Monitor Serie dice | Qué revisar |
|---|---|
| `fallo (-1 connection refused)` | backend apagado, IP equivocada o firewall cerrado |
| `HTTP 401` / `403` | `SENSOR_TOKEN` distinto del del servidor |
| `HTTP 404` | ese número de cubículo no existe en el servidor |
| `Sin WiFi` | red de 5 GHz o clave mal escrita |
| `DFPlayer no responde` | TX/RX cruzados, microSD sin formato FAT32 |

Sin WiFi o sin servidor, la maqueta no se queda muda: funciona como el sketch original (movimiento → luz azul por 1 minuto)
y vuelve a sincronizarse sola cuando el servidor responde.
