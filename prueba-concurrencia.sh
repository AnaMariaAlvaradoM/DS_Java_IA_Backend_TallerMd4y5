
#!/usr/bin/env bash

# Solicitar el JWT sin mostrarlo en pantalla
read -r -s -p "Pega el token JWT del dueño: " TOKEN
echo

URL="http://localhost:8080/api/citas"

# Archivos temporales para las respuestas
TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"; unset TOKEN' EXIT

# Ambas solicitudes intentan reservar el mismo horario
CITA_A='{
  "clienteNombre": "Prueba Concurrente A",
  "clienteTelefono": "3110001001",
  "servicioId": 1,
  "estilistaId": 4,
  "inicio": "2026-11-27T14:00:00"
}'

CITA_B='{
  "clienteNombre": "Prueba Concurrente B",
  "clienteTelefono": "3110001002",
  "servicioId": 1,
  "estilistaId": 4,
  "inicio": "2026-11-27T14:00:00"
}'

# Ejecutar ambas solicitudes en paralelo
curl -sS -X POST "$URL" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "$CITA_A" \
  -o "$TMP_DIR/a.json" \
  -w "%{http_code}" > "$TMP_DIR/a.status" &

PID_A=$!

curl -sS -X POST "$URL" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "$CITA_B" \
  -o "$TMP_DIR/b.json" \
  -w "%{http_code}" > "$TMP_DIR/b.status" &

PID_B=$!

wait "$PID_A"
wait "$PID_B"

echo "Resultado solicitud A: $(cat "$TMP_DIR/a.status")"
cat "$TMP_DIR/a.json"
echo

echo "Resultado solicitud B: $(cat "$TMP_DIR/b.status")"
cat "$TMP_DIR/b.json"
echo
