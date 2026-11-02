#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script para sincronizar las 3 listas oficiales de IDMJI:
1. Preguntas y Respuestas (121 videos)
2. Estudios Bíblicos Español (187 videos)
3. Transmisiones en Vivo (870 videos)
"""

import subprocess
import json
import os

BASE_DIR = "/home/rocua/Proyectos/IDMJI/Compendio_Doctrinal_IDMJI"
METADATA_DIR = os.path.join(BASE_DIR, "metadata")

PLAYLISTS = {
    "preguntas_y_respuestas": {
        "nombre": "Preguntas y Respuestas Hna. María Luisa Piraquive",
        "id": "PLS5oL9JH3rcv7GNbuP0FGu3L42cQqvKx-",
        "url": "https://www.youtube.com/playlist?list=PLS5oL9JH3rcv7GNbuP0FGu3L42cQqvKx-"
    },
    "estudios_biblicos": {
        "nombre": "Estudios Bíblicos Español - Web",
        "id": "PLS5oL9JH3rctscuHO3DerB-jnLBYVDew8",
        "url": "https://www.youtube.com/playlist?list=PLS5oL9JH3rctscuHO3DerB-jnLBYVDew8"
    },
    "transmisiones_en_vivo": {
        "nombre": "Transmisiones en vivo - IDMJI",
        "id": "PLS5oL9JH3rcuydz5HIM9UF-LJis5EtuMQ",
        "url": "https://www.youtube.com/playlist?list=PLS5oL9JH3rcuydz5HIM9UF-LJis5EtuMQ"
    }
}

def sync_playlist(key, info):
    print(f"[*] Obteniendo lista de videos para: {info['nombre']}...")
    cmd = [
        "/home/rocua/.local/bin/yt-dlp",
        "--flat-playlist",
        "--print", "%(id)s\t%(title)s\t%(duration)s",
        info["url"]
    ]
    res = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    if res.returncode != 0:
        print(f"[!] Error al obtener {info['nombre']}: {res.stderr}")
        return []
    
    videos = []
    lines = res.stdout.strip().split("\n")
    for line in lines:
        if not line.strip():
            continue
        parts = line.split("\t")
        vid_id = parts[0].strip()
        title = parts[1].strip() if len(parts) > 1 else ""
        duration = parts[2].strip() if len(parts) > 2 else "0"
        videos.append({
            "id": vid_id,
            "titulo": title,
            "url": f"https://www.youtube.com/watch?v={vid_id}",
            "duracion": duration,
            "playlist": key
        })
    
    out_file = os.path.join(METADATA_DIR, f"{key}.json")
    with open(out_file, "w", encoding="utf-8") as f:
        json.dump(videos, f, ensure_ascii=False, indent=2)
    print(f"[+] Guardados {len(videos)} videos en {out_file}")
    return videos

if __name__ == "__main__":
    os.makedirs(METADATA_DIR, exist_ok=True)
    for k, v in PLAYLISTS.items():
        sync_playlist(k, v)
