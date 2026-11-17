
#!/usr/bin/env bash

set -u

URL="http://localhost:8080/api/publico/salones/barberia-la-navaja/reservas"

# Archivos temporales
TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"' EXIT

# Dos clientes intentan reservar el mismo horario
CITA_A='{
  "clienteNombre": "Prueba Publica A",
  "clienteTelefono": "3110002001",
  "servicioId": 1,
  "estilistaId": 3,
  "inicio": "2026-12-18T15:00:00"
}'

CITA_B='{
  "clienteNombre": "Prueba Publica B",
  "clienteTelefono": "3110002002",
  "servicioId": 1,
  "estilistaId": 3,
  "inicio": "2026-12-18T15:00:00"
}'

# Ejecutar ambas solicitudes en paralelo
curl -sS -X POST "$URL" \
  -H "Content-Type: application/json" \
  -d "$CITA_A" \
  -o "$TMP_DIR/a.json" \
  -w "%{http_code}" > "$TMP_DIR/a.status" &

PID_A=$!

curl -sS -X POST "$URL" \
  -H "Content-Type: application/json" \
  -d "$CITA_B" \
  -o "$TMP_DIR/b.json" \
  -w "%{http_code}" > "$TMP_DIR/b.status" &

PID_B=$!

wait "$PID_A" || true
wait "$PID_B" || true

ESTADO_A=$(cat "$TMP_DIR/a.status")
ESTADO_B=$(cat "$TMP_DIR/b.status")

echo "Solicitud A: HTTP $ESTADO_A"
echo "Solicitud B: HTTP $ESTADO_B"

if [[ "$ESTADO_A" == "201" && "$ESTADO_B" == "409" ]] ||
   [[ "$ESTADO_A" == "409" && "$ESTADO_B" == "201" ]]; then
    echo "SUCCESS: Solo una reserva fue aceptada."
else
    echo "REVISAR: Resultado diferente al esperado."
    echo "Respuesta A:"
    cat "$TMP_DIR/a.json"
    echo
    echo "Respuesta B:"
    cat "$TMP_DIR/b.json"
    echo
fi
