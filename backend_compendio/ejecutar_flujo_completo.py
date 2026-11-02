#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Ejecutor maestro de extracción y compilación secuencial para IDMJI.
Etapa 1: Preguntas y Respuestas (121 videos)
Etapa 2: Estudios Bíblicos (187 videos)
Etapa 3: Transmisiones en Vivo (870 videos)
"""

import sys
import time
import subprocess
import os

BASE_DIR = "/home/rocua/Proyectos/IDMJI/Compendio_Doctrinal_IDMJI"
LOG_FILE = os.path.join(BASE_DIR, "progreso_extraccion.log")

def log(msg):
    timestamp = time.strftime("[%Y-%m-%d %H:%M:%S]")
    line = f"{timestamp} {msg}"
    print(line, flush=True)
    with open(LOG_FILE, "a", encoding="utf-8") as f:
        f.write(line + "\n")

def ejecutar_etapa(nombre_playlist):
    log(f"=== INICIANDO DESCARGA: {nombre_playlist.upper()} ===")
    cmd_descarga = [
        sys.executable,
        os.path.join(BASE_DIR, "descargar_transcripciones.py"),
        nombre_playlist
    ]
    p = subprocess.run(cmd_descarga, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    log(p.stdout)
    if p.returncode != 0:
        log(f"[ERROR] en descarga {nombre_playlist}: {p.stderr}")

    log(f"=== COMPILANDO COMPENDIO BÍBLICO TRAS {nombre_playlist.upper()} ===")
    cmd_compila = [
        sys.executable,
        os.path.join(BASE_DIR, "compilar_compendio.py")
    ]
    p_comp = subprocess.run(cmd_compila, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    log(p_comp.stdout)
    log(f"=== FINALIZADA ETAPA: {nombre_playlist.upper()} ===\n")

if __name__ == "__main__":
    log(">>> INICIO DEL FLUJO COMPLETO DE COMPILACIÓN IDMJI <<<")
    # Etapa 1
    ejecutar_etapa("preguntas_y_respuestas")
    # Etapa 2
    ejecutar_etapa("estudios_biblicos")
    # Etapa 3
    ejecutar_etapa("transmisiones_en_vivo")
    log(">>> FLUJO COMPLETO FINALIZADO EXITOSAMENTE <<<")
