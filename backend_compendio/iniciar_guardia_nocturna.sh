#!/usr/bin/env bash
# Script de inicio desatendido para la guardia nocturna IDMJI
DIR="/home/rocua/Proyectos/IDMJI/Compendio_Doctrinal_IDMJI"
cd "$DIR" || exit 1

# Prevenir suspensión del sistema operativo mientras corre el proceso
systemd-inhibit --what=idle:sleep --who="Compilador IDMJI" --why="Descargando y compilando transcripciones biblicas" \
  python3 "$DIR/descargador_nocturno.py" >> "$DIR/progreso_nocturno.log" 2>&1
