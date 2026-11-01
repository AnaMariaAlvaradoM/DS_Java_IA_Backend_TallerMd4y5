#!/usr/bin/env bash
# Lee del pom.xml del estudiante qué versión de Java pide y elige el JDK que se instala.
# Así cada estudiante se califica con su versión (17, 21, 25…) y no con una fija.
#
# Uso: bash .github/calificador/version-java.sh [ruta/al/pom.xml]
# Imprime "pedida=N" y "usada=N"; en GitHub Actions también los deja como salidas del paso.

set -u

POM="${1:-pom.xml}"
MINIMA=17      # Spring Boot 3 y 4 no corren con menos
MAXIMA=27      # la más nueva que publica Temurin
POR_DEFECTO=21 # si el pom no dice ninguna versión

pedida=""
if [ -f "$POM" ]; then
  # Quita los comentarios <!-- … --> y busca todas las etiquetas que fijan la versión de Java.
  # Se toma la mayor: si dice java.version 25 y release 17, hace falta un JDK 25.
  valores=$(perl -0777 -pe 's/<!--.*?-->//gs' "$POM" \
    | grep -oE '<(java\.version|maven\.compiler\.(release|source|target)|release|source|target)>[[:space:]]*(1\.)?[0-9]{1,3}[[:space:]]*</' \
    | grep -oE '>[[:space:]]*(1\.)?[0-9]{1,3}' \
    | sed -E 's/^>[[:space:]]*//; s/^1\.//')
  for v in $valores; do
    v=$((10#$v))
    if [ -z "$pedida" ] || [ "$v" -gt "$pedida" ]; then
      pedida=$v
    fi
  done
fi

if [ -z "$pedida" ]; then
  usada=$POR_DEFECTO
elif [ "$pedida" -lt "$MINIMA" ]; then
  usada=$MINIMA
elif [ "$pedida" -gt "$MAXIMA" ]; then
  usada=$MAXIMA
else
  usada=$pedida
fi

echo "pedida=$pedida"
echo "usada=$usada"
if [ -n "${GITHUB_OUTPUT:-}" ]; then
  {
    echo "pedida=$pedida"
    echo "usada=$usada"
  } >> "$GITHUB_OUTPUT"
fi
