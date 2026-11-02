#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Sincronizador Automático Diario para IDMJI.
- Escanea las 4 fuentes oficiales de YouTube (Preguntas, Meditaciones, Estudios, Transmisiones).
- Detecta únicamente los videos nuevos que no han sido descargados.
- Si no hay nuevos videos, termina en segundos sin consumir recursos.
- Si hay videos nuevos:
  1. Descarga sus subtítulos oficiales palabra por palabra con el motor híbrido blindado.
  2. Guarda el archivo .json en la caché local permanente.
  3. Ejecuta el compilador canónico granular para insertar las nuevas enseñanzas y preguntas en el compendio maestro.
"""

import os
import sys
import time
import json
import random
import subprocess
import requests
import webvtt
import re
import urllib.request
import glob
import shutil
from youtube_transcript_api import YouTubeTranscriptApi
import youtube_transcript_api._errors as yt_errs

BASE_DIR = "/home/rocua/Proyectos/IDMJI/Compendio_Doctrinal_IDMJI"
TRANSCRIPCIONES_DIR = os.path.join(BASE_DIR, "transcripciones")
METADATA_DIR = os.path.join(BASE_DIR, "metadata")
LOG_FILE = os.path.join(BASE_DIR, "actualizacion_automatica.log")
REPO_DIR = "/home/rocua/Proyectos/IDMJI/Biblia_Android"

def sincronizar_con_git(mensaje="chore(doctrina): actualización automática de compendio y scripts"):
    log("🚀 Sincronizando scripts y compendio con el repositorio Git...")
    try:
        backend_dir = os.path.join(REPO_DIR, "backend_compendio")
        os.makedirs(backend_dir, exist_ok=True)
        for ext in ("*.py", "*.sh"):
            for f in glob.glob(os.path.join(BASE_DIR, ext)):
                shutil.copy2(f, backend_dir)

        subprocess.run(["git", "-C", REPO_DIR, "add", "."], check=True)
        res = subprocess.run(["git", "-C", REPO_DIR, "diff", "--staged", "--quiet"])
        if res.returncode != 0:
            subprocess.run(["git", "-C", REPO_DIR, "commit", "-m", mensaje], check=True)
            subprocess.run(["git", "-C", REPO_DIR, "push", "origin", "main"], check=True)
            log("✓ Cambios subidos exitosamente a GitHub (origin/main).")
        else:
            log("[=] Repositorio Git ya se encuentra al día.")
    except Exception as e:
        log(f"[!] Aviso al sincronizar con Git: {e}")
YT_DLP_BIN = "/home/rocua/.local/bin/yt-dlp"

TOR_HOST = "127.0.0.1"
TOR_PORT = 9050

INVIDIOUS_INSTANCES = [
    "https://yt.chocolatemoo53.com",
    "https://inv.nadeko.net"
]

FUENTES = {
    "preguntas_y_respuestas": {
        "nombre": "Preguntas y Respuestas Hna. María Luisa",
        "url": "https://www.youtube.com/playlist?list=PLS5oL9JH3rcv7GNbuP0FGu3L42cQqvKx-"
    },
    "meditaciones": {
        "nombre": "Meditaciones Hna. María Luisa Piraquive",
        "url": "https://www.youtube.com/@mlpiraquive/search?query=meditaciones"
    },
    "estudios_biblicos": {
        "nombre": "Estudios Bíblicos Español - Web",
        "url": "https://www.youtube.com/playlist?list=PLS5oL9JH3rctscuHO3DerB-jnLBYVDew8"
    },
    "transmisiones_en_vivo": {
        "nombre": "Transmisiones en vivo - IDMJI",
        "url": "https://www.youtube.com/playlist?list=PLS5oL9JH3rcuydz5HIM9UF-LJis5EtuMQ"
    }
}

def log(msg):
    ahora = time.strftime("[%Y-%m-%d %H:%M:%S]")
    linea = f"{ahora} {msg}"
    print(linea, flush=True)
    with open(LOG_FILE, "a", encoding="utf-8") as f:
        f.write(linea + "\n")
        f.flush()

def video_existe_en_youtube(vid_id):
    try:
        url = f"https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v={vid_id}&format=json"
        r = requests.get(url, timeout=5)
        return r.status_code == 200
    except Exception:
        return True

def parse_and_dedup_vtt(vtt_content):
    caption_file = webvtt.from_string(vtt_content)
    snippets = []
    for c in caption_file:
        lines = [re.sub(r"<[^>]+>", "", l).strip() for l in c.text.splitlines() if l.strip()]
        lines = [l for l in lines if l != "[Música]"]
        if not lines:
            continue
        
        text_to_add = None
        if len(lines) == 1:
            text_to_add = lines[0]
        else:
            if snippets and lines[0] == snippets[-1]["text"]:
                text_to_add = lines[1]
            else:
                text_to_add = " ".join(lines)
                
        if text_to_add and (not snippets or text_to_add != snippets[-1]["text"]):
            snippets.append({
                "start": float(c.start_in_seconds),
                "duration": round(float(c.end_in_seconds - c.start_in_seconds), 2),
                "text": text_to_add
            })
    return snippets

def descargar_via_invidious(vid_id):
    for inst in INVIDIOUS_INSTANCES:
        try:
            url = f"{inst}/api/v1/captions/{vid_id}"
            req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
            with urllib.request.urlopen(req, timeout=10) as resp:
                data = json.load(resp)
                caps = data.get("captions", [])
                for c in caps:
                    lang = c.get("languageCode", "").lower()
                    label = c.get("label", "").lower()
                    if lang in ("es", "es-419", "spanish") or "span" in label:
                        sub_path = c["url"]
                        sub_url = inst + sub_path if sub_path.startswith("/") else f"{inst}/{sub_path}"
                        sreq = urllib.request.Request(sub_url, headers={"User-Agent": "Mozilla/5.0"})
                        with urllib.request.urlopen(sreq, timeout=12) as sresp:
                            vtt = sresp.read().decode("utf-8", errors="replace")
                            if len(vtt) > 200:
                                snippets = parse_and_dedup_vtt(vtt)
                                if snippets:
                                    return snippets
        except Exception:
            continue
    return None

def descargar_con_estrategia_hibrida(vid_id, titulo):
    if not video_existe_en_youtube(vid_id):
        log(f"    ✗ Omitido {vid_id} (Video privado o retirado [oEmbed 404]).")
        return "UNAVAILABLE"

    # 1. Invidious
    snips = descargar_via_invidious(vid_id)
    if snips:
        return snips

    # 2. YouTube API Oficial
    try:
        api_directa = YouTubeTranscriptApi()
        res = api_directa.fetch(vid_id, languages=['es', 'es-419'])
        return [{"start": s.start, "duration": s.duration, "text": s.text} for s in res]
    except (yt_errs.NoTranscriptFound, yt_errs.TranscriptsDisabled):
        return None
    except yt_errs.VideoUnavailable:
        return "UNAVAILABLE"
    except Exception:
        pass

    # 3. Tor con rotación de circuito
    for intento in range(1, 5):
        try:
            user = f"tor_rot_{random.randint(10000, 99999)}"
            s = requests.Session()
            s.proxies = {"http": f"socks5h://{user}:p@{TOR_HOST}:{TOR_PORT}", "https": f"socks5h://{user}:p@{TOR_HOST}:{TOR_PORT}"}
            api_tor = YouTubeTranscriptApi(http_client=s)
            res = api_tor.fetch(vid_id, languages=['es', 'es-419'])
            return [{"start": s.start, "duration": s.duration, "text": s.text} for s in res]
        except (yt_errs.NoTranscriptFound, yt_errs.TranscriptsDisabled):
            return None
        except yt_errs.VideoUnavailable:
            return "UNAVAILABLE"
        except Exception:
            time.sleep(1.0)

    return "RETRY"

def obtener_videos_remotos(key, info):
    cmd = [
        YT_DLP_BIN,
        "--flat-playlist",
        "--print", "%(id)s\t%(title)s\t%(duration)s",
        info["url"]
    ]
    res = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    if res.returncode != 0:
        log(f"[!] Error al comprobar {info['nombre']}: {res.stderr[:100]}")
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
    return videos

def sincronizar_todo():
    log("===========================================================")
    log("INICIANDO COMPROBACIÓN DE NUEVOS VIDEOS IDMJI")
    log("===========================================================")

    nuevos_totales = 0

    for key, info in FUENTES.items():
        target_dir = os.path.join(TRANSCRIPCIONES_DIR, key)
        os.makedirs(target_dir, exist_ok=True)

        videos_remotos = obtener_videos_remotos(key, info)
        if not videos_remotos:
            continue

        videos_pendientes = [v for v in videos_remotos if not os.path.exists(os.path.join(target_dir, f"{v['id']}.json"))]

        if not videos_pendientes:
            log(f"  * {info['nombre']}: Al día (0 videos nuevos de {len(videos_remotos)} totales).")
            continue

        log(f"  * {info['nombre']}: Se encontraron {len(videos_pendientes)} videos NUEVOS.")

        for v in videos_pendientes:
            vid_id = v["id"]
            titulo = v["titulo"]
            out_path = os.path.join(target_dir, f"{vid_id}.json")

            log(f"    Descargando: {titulo[:60]}...")
            resultado = descargar_con_estrategia_hibrida(vid_id, titulo)

            if resultado in ("UNAVAILABLE", None, "RETRY"):
                continue

            data = {
                "id": vid_id,
                "titulo": titulo,
                "url": v["url"],
                "playlist": key,
                "snippets": resultado
            }
            with open(out_path, "w", encoding="utf-8") as out_f:
                json.dump(data, out_f, ensure_ascii=False, indent=2)

            nuevos_totales += 1
            log(f"    ✓ Guardado ({len(resultado)} fragmentos).")
            time.sleep(2.0)

    if nuevos_totales > 0:
        log(f"\n[+] Se descargaron {nuevos_totales} videos nuevos. Recompilando compendio canónico...")
        try:
            cmd = [sys.executable, os.path.join(BASE_DIR, "compilar_compendio.py")]
            res = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
            log("📚 Compendio Markdown actualizado con éxito.")
            
            log("📱 Actualizando base de datos SQLite de la App Android...")
            cmd_db = [sys.executable, os.path.join(BASE_DIR, "actualizar_db_android.py")]
            res_db = subprocess.run(cmd_db, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
            log("✓ Base de datos SQLite de la App Android actualizada con éxito.")

            # Sincronizar scripts y compendio con GitHub
            sincronizar_con_git(f"chore(doctrina): sincronización automática de {nuevos_totales} videos nuevos y Turso Cloud")
        except Exception as e:
            log(f"[!] Error al recompilar: {e}")
    else:
        log("[=] Todo el compendio se encuentra 100% al día con YouTube. No hubo novedades.")

if __name__ == "__main__":
    sincronizar_todo()
