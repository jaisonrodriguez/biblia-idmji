#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Descargador oficial de transcripciones de YouTube para IDMJI.
Incluye pausas suaves y reintentos para respetar las cuotas de YouTube.
"""

import os
import json
import time
import sys
from youtube_transcript_api import YouTubeTranscriptApi

BASE_DIR = "/home/rocua/Proyectos/IDMJI/Compendio_Doctrinal_IDMJI"
METADATA_DIR = os.path.join(BASE_DIR, "metadata")
TRANSCRIPCIONES_DIR = os.path.join(BASE_DIR, "transcripciones")

api = YouTubeTranscriptApi()

def descargar_playlist(key, limite=None, pausa=2.0):
    meta_file = os.path.join(METADATA_DIR, f"{key}.json")
    if not os.path.exists(meta_file):
        print(f"[!] No existe {meta_file}. Ejecuta primero sincronizar_listas.py")
        return

    with open(meta_file, "r", encoding="utf-8") as f:
        videos = json.load(f)

    target_dir = os.path.join(TRANSCRIPCIONES_DIR, key)
    os.makedirs(target_dir, exist_ok=True)

    total = len(videos) if limite is None else min(len(videos), limite)
    print(f"\n=======================================================")
    print(f"[*] Iniciando/Reanudando descarga: {key.upper()} ({total} videos)")
    print(f"=======================================================")

    exitos = 0
    sin_subtitulos = 0
    omitidos = 0

    for i, v in enumerate(videos[:total], 1):
        vid_id = v["id"]
        titulo = v["titulo"]
        out_path = os.path.join(target_dir, f"{vid_id}.json")

        if os.path.exists(out_path):
            omitidos += 1
            continue

        print(f"[{i}/{total}] Descargando: {titulo[:55]}... ", end="", flush=True)
        intentos = 0
        descargado = False

        while intentos < 3 and not descargado:
            try:
                transcript = api.fetch(vid_id, languages=['es', 'es-419'])
                data = {
                    "id": vid_id,
                    "titulo": titulo,
                    "url": v["url"],
                    "playlist": key,
                    "snippets": [{"start": s.start, "duration": s.duration, "text": s.text} for s in transcript]
                }
                with open(out_path, "w", encoding="utf-8") as out_f:
                    json.dump(data, out_f, ensure_ascii=False, indent=2)
                print("✓ OK")
                exitos += 1
                descargado = True
                time.sleep(pausa)
            except Exception as e:
                err_name = type(e).__name__
                if "RequestBlocked" in err_name or "TooManyRequests" in str(e):
                    intentos += 1
                    print(f" [Límite temporal detectado, esperando 45s (intento {intentos}/3)]...", end="", flush=True)
                    time.sleep(45)
                else:
                    print(f"✗ ({err_name})")
                    sin_subtitulos += 1
                    break

        if not descargado and intentos >= 3:
            print("✗ (Pausado por cuota de red de YouTube)")
            break

    print(f"\n[+] Resumen {key}:")
    print(f"    - Nuevos descargados: {exitos}")
    print(f"    - Ya existían previamente: {omitidos}")
    print(f"    - Sin subtítulos: {sin_subtitulos}")

if __name__ == "__main__":
    opcion = sys.argv[1] if len(sys.argv) > 1 else "preguntas_y_respuestas"
    limite = int(sys.argv[2]) if len(sys.argv) > 2 else None
    pausa = float(sys.argv[3]) if len(sys.argv) > 3 else 2.0

    descargar_playlist(opcion, limite, pausa)
