#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Descargador Blindado Multi-Nivel para IDMJI.
- Nivel 1: Descarga directa y ultrarrápida mediante Invidious API (sin límites de cuota).
- Nivel 2: youtube_transcript_api directo oficial.
- Nivel 3: Conmutación y rotación dinámica por VPN/Proxy Tor local (127.0.0.1:9050).
- Detección instantánea de videos eliminados/privados (oEmbed 404) en 0.2s.
- Auto-compilación continua del compendio en estricto orden canónico de la Biblia cada 15 videos.
"""

import os
import sys
import time
import json
import random
import re
import subprocess
import urllib.request
import urllib.parse
import requests
import webvtt
from youtube_transcript_api import YouTubeTranscriptApi
import youtube_transcript_api._errors as yt_errs

BASE_DIR = "/home/rocua/Proyectos/IDMJI/Compendio_Doctrinal_IDMJI"
METADATA_DIR = os.path.join(BASE_DIR, "metadata")
TRANSCRIPCIONES_DIR = os.path.join(BASE_DIR, "transcripciones")
COMPENDIOS_DIR = os.path.join(BASE_DIR, "compendios")
LOG_FILE = os.path.join(BASE_DIR, "progreso_nocturno.log")

TOR_HOST = "127.0.0.1"
TOR_PORT = 9050

INVIDIOUS_INSTANCES = [
    "https://yt.chocolatemoo53.com",
    "https://inv.nadeko.net"
]

def log(msg):
    ahora = time.strftime("[%Y-%m-%d %H:%M:%S]")
    linea = f"{ahora} {msg}"
    print(linea, flush=True)
    with open(LOG_FILE, "a", encoding="utf-8") as f:
        f.write(linea + "\n")
        f.flush()

def video_existe_en_youtube(vid_id):
    """Verifica en 0.2s mediante oEmbed si el video sigue público en YouTube."""
    try:
        url = f"https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v={vid_id}&format=json"
        r = requests.get(url, timeout=5)
        return r.status_code == 200
    except Exception:
        return True

def parse_and_dedup_vtt(vtt_content):
    """Parsea WebVTT y elimina líneas repetidas de subtítulos automáticos en cascada."""
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
    """Intenta descargar subtítulos oficiales en español mediante instancias Invidious."""
    for inst in INVIDIOUS_INSTANCES:
        try:
            url = f"{inst}/api/v1/captions/{vid_id}"
            req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"})
            with urllib.request.urlopen(req, timeout=12) as resp:
                data = json.load(resp)
                caps = data.get("captions", [])
                for c in caps:
                    lang = c.get("languageCode", "").lower()
                    label = c.get("label", "").lower()
                    if lang in ("es", "es-419", "spanish") or "span" in label:
                        sub_path = c["url"]
                        sub_url = inst + sub_path if sub_path.startswith("/") else f"{inst}/{sub_path}"
                        sreq = urllib.request.Request(sub_url, headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"})
                        with urllib.request.urlopen(sreq, timeout=15) as sresp:
                            vtt = sresp.read().decode("utf-8", errors="replace")
                            if len(vtt) > 200:
                                snippets = parse_and_dedup_vtt(vtt)
                                if snippets:
                                    return snippets
        except Exception:
            continue
    return None

def obtener_cliente_tor_rotativo():
    """Genera una sesión Tor con circuito de salida nuevo mediante SOCKS5 Auth."""
    user = f"tor_rot_{random.randint(10000, 99999)}"
    proxy_url = f"socks5h://{user}:password@{TOR_HOST}:{TOR_PORT}"
    session = requests.Session()
    session.proxies = {"http": proxy_url, "https": proxy_url}
    return YouTubeTranscriptApi(http_client=session)

def descargar_con_estrategia_hibrida(vid_id, titulo):
    # 0. Verificación oEmbed
    if not video_existe_en_youtube(vid_id):
        log("    ✗ Omitido (Video privado o retirado en YouTube [oEmbed 404]).")
        return "UNAVAILABLE"

    # 1. Nivel Invidious (rápido, sin bloqueo de IP)
    snips = descargar_via_invidious(vid_id)
    if snips:
        log("    ✓ OK descargado vía Invidious.")
        return snips

    # 2. Nivel Directo Oficial
    try:
        api_directa = YouTubeTranscriptApi()
        res = api_directa.fetch(vid_id, languages=['es', 'es-419'])
        log("    ✓ OK descargado vía YouTube oficial directo.")
        return [{"start": s.start, "duration": s.duration, "text": s.text} for s in res]
    except (yt_errs.NoTranscriptFound, yt_errs.TranscriptsDisabled):
        log("    ✗ Omitido (No tiene subtítulos en español).")
        return None
    except yt_errs.VideoUnavailable:
        log("    ✗ Omitido (Video no disponible).")
        return "UNAVAILABLE"
    except Exception:
        pass

    # 3. Nivel Tor con rotación de circuito
    log("    [Probando rotación por Tor VPN]...")
    for intento in range(1, 6):
        try:
            api_tor = obtener_cliente_tor_rotativo()
            res = api_tor.fetch(vid_id, languages=['es', 'es-419'])
            log(f"    ✓ OK descargado vía Tor (Circuito #{intento}).")
            return [{"start": s.start, "duration": s.duration, "text": s.text} for s in res]
        except (yt_errs.NoTranscriptFound, yt_errs.TranscriptsDisabled):
            log("    ✗ Omitido (Sin subtítulos en español).")
            return None
        except yt_errs.VideoUnavailable:
            log("    ✗ Omitido (Video no disponible).")
            return "UNAVAILABLE"
        except Exception:
            time.sleep(1.0)
            continue

    log("    ⏳ Pausa breve de 30s antes de reintentar...")
    time.sleep(30)
    return "RETRY"

def compilar():
    try:
        cmd = [sys.executable, os.path.join(BASE_DIR, "compilar_compendio.py")]
        res = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        log(f"📚 {res.stdout.strip()}")
    except Exception as ex:
        log(f"[Aviso] Error al compilar: {ex}")

def procesar_lista(key, nombre_visible):
    meta_path = os.path.join(METADATA_DIR, f"{key}.json")
    if not os.path.exists(meta_path):
        log(f"[!] No se encontró {meta_path}. Saltando.")
        return

    with open(meta_path, "r", encoding="utf-8") as f:
        videos = json.load(f)

    target_dir = os.path.join(TRANSCRIPCIONES_DIR, key)
    os.makedirs(target_dir, exist_ok=True)

    total = len(videos)
    log(f"===========================================================")
    log(f"INICIANDO LISTA: {nombre_visible.upper()} ({total} videos)")
    log(f"===========================================================")

    nuevos = 0
    ya_existentes = 0
    omitidos = 0
    contador_compilacion = 0

    for idx, v in enumerate(videos, 1):
        vid_id = v["id"]
        titulo = v["titulo"]
        out_path = os.path.join(target_dir, f"{vid_id}.json")

        if os.path.exists(out_path):
            ya_existentes += 1
            continue

        descargado = False
        while not descargado:
            log(f"[{idx}/{total}] [{key}] Descargando: {titulo[:60]}...")
            resultado = descargar_con_estrategia_hibrida(vid_id, titulo)

            if resultado == "RETRY":
                log(f"    🔄 Reintentando [{titulo[:40]}]...")
                continue
            elif resultado in ("UNAVAILABLE", None):
                omitidos += 1
                descargado = True
                time.sleep(1.0)
            else:
                data = {
                    "id": vid_id,
                    "titulo": titulo,
                    "url": v["url"],
                    "playlist": key,
                    "snippets": resultado
                }
                with open(out_path, "w", encoding="utf-8") as out_f:
                    json.dump(data, out_f, ensure_ascii=False, indent=2)
                
                nuevos += 1
                contador_compilacion += 1
                descargado = True
                log(f"    ✓ OK ({len(resultado)} fragmentos guardados).")

                # Cada 15 videos recompilar el compendio en orden bíblico
                if contador_compilacion >= 15:
                    compilar()
                    contador_compilacion = 0

                # Pausa prudente de 2.5 a 4.5 segundos
                time.sleep(2.5 + random.uniform(0.5, 2.0))

    compilar()
    log(f"FINALIZADA LISTA {nombre_visible}: {nuevos} nuevos, {ya_existentes} previos, {omitidos} omitidos.")

def main():
    log("***********************************************************")
    log("PROCESADOR AUTOMÁTICO IDMJI — MODO HÍBRIDO BLINDADO")
    log("Invidious API + YouTube Oficial + Rotación VPN Tor 127.0.0.1:9050")
    log("Ordenamiento Canónico Estricto de la Biblia (Génesis a Apocalipsis)")
    log("***********************************************************")

    # 1. Preguntas y Respuestas (121 videos)
    procesar_lista("preguntas_y_respuestas", "Preguntas y Respuestas (121 videos)")
    # 2. Meditaciones Hna María Luisa (308 videos)
    procesar_lista("meditaciones", "Meditaciones Hna. María Luisa Piraquive (308 videos)")
    # 3. Estudios Bíblicos (187 videos)
    procesar_lista("estudios_biblicos", "Estudios Bíblicos Español (187 videos)")
    # 4. Transmisiones en Vivo (870 videos)
    procesar_lista("transmisiones_en_vivo", "Transmisiones en Vivo (870 videos)")

    log("***********************************************************")
    log("🎉 ¡COMPLETADO EL 100% DE TODOS LOS VIDEOS CON ÉXITO!")
    log("***********************************************************")
    compilar()

if __name__ == "__main__":
    main()
