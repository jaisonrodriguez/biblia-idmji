#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
actualizar_db_android.py
Actualiza la base de datos SQLite preempaquetada de la app Android (biblia_rvr1960.db)
con las más de 8,000 intervenciones doctrinales granulares, versículo por versículo,
de la Hna. María Luisa Piraquive.
"""

import os
import sys
import glob
import json
import sqlite3
import unicodedata
import time

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.append(SCRIPT_DIR)
import compilar_compendio
import sincronizar_turso

DB_PATH = "/home/rocua/Proyectos/IDMJI/Biblia_Android/app/src/main/assets/biblia_rvr1960.db"
TRANSCRIPCIONES_DIR = os.path.join(SCRIPT_DIR, "transcripciones")

def norm(s):
    return unicodedata.normalize("NFD", s).encode("ascii", "ignore").decode("utf-8").lower().strip()

def generar_transcripcion(snippets, base_url):
    lines = []
    bloque_texto = []
    tiempo_inicio = None
    for snip in snippets:
        if tiempo_inicio is None:
            tiempo_inicio = int(snip["start"])
        bloque_texto.append(snip["text"].strip())
        if len(bloque_texto) >= 6 or (int(snip["start"]) - tiempo_inicio) >= 35:
            t_str = compilar_compendio.formatear_tiempo(tiempo_inicio)
            url_t = f"{base_url}&t={tiempo_inicio}s"
            lines.append(f"[[{t_str}]]({url_t}) {' '.join(bloque_texto)}")
            bloque_texto = []
            tiempo_inicio = None
    if bloque_texto:
        t_str = compilar_compendio.formatear_tiempo(tiempo_inicio or 0)
        url_t = f"{base_url}&t={tiempo_inicio or 0}s"
        lines.append(f"[[{t_str}]]({url_t}) {' '.join(bloque_texto)}")
    return "\n\n".join(lines)

def actualizar_db():
    print("=" * 65)
    print("ACTUALIZADOR DE COMPENDIO DOCTRINAL PARA LA APP ANDROID")
    print("=" * 65)

    if not os.path.exists(DB_PATH):
        print(f"[!] Error: No se encontró la base de datos en {DB_PATH}")
        sys.exit(1)

    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()

    # Mapeo de libros de la base de datos
    db_books = cur.execute("SELECT id, nombre FROM libros ORDER BY id").fetchall()
    db_books_map = {norm(name): (b_id, name) for b_id, name in db_books}
    print(f"[+] {len(db_books)} libros canónicos cargados desde la base de datos.")

    # Verificar videos ya indexados en la base de datos
    cur.execute("CREATE TABLE IF NOT EXISTS compendio_idmji (id INTEGER PRIMARY KEY AUTOINCREMENT, tipo TEXT NOT NULL, libro_id INTEGER, capitulo INTEGER, versiculo_inicio INTEGER, versiculo_fin INTEGER, cita TEXT, titulo TEXT NOT NULL, video_url TEXT, video_id TEXT, seccion TEXT, transcripcion TEXT);")
    cur.execute("CREATE INDEX IF NOT EXISTS idx_compendio_verse ON compendio_idmji(libro_id, capitulo, versiculo_inicio, versiculo_fin);")
    cur.execute("CREATE INDEX IF NOT EXISTS idx_compendio_tipo ON compendio_idmji(tipo);")
    cur.execute("CREATE INDEX IF NOT EXISTS idx_compendio_libro ON compendio_idmji(libro_id);")

    rebuild_all = "--rebuild" in sys.argv or "--rebuild-all" in sys.argv
    if rebuild_all:
        print("[!] Modo Reconstrucción Total activado: Reseteando tabla compendio_idmji...")
        cur.execute("DROP TABLE IF EXISTS compendio_idmji;")
        cur.execute("CREATE TABLE compendio_idmji (id INTEGER PRIMARY KEY AUTOINCREMENT, tipo TEXT NOT NULL, libro_id INTEGER, capitulo INTEGER, versiculo_inicio INTEGER, versiculo_fin INTEGER, cita TEXT, titulo TEXT NOT NULL, video_url TEXT, video_id TEXT, seccion TEXT, transcripcion TEXT);")
        cur.execute("CREATE INDEX IF NOT EXISTS idx_compendio_verse ON compendio_idmji(libro_id, capitulo, versiculo_inicio, versiculo_fin);")
        cur.execute("CREATE INDEX IF NOT EXISTS idx_compendio_tipo ON compendio_idmji(tipo);")
        cur.execute("CREATE INDEX IF NOT EXISTS idx_compendio_libro ON compendio_idmji(libro_id);")
        cur.execute("DELETE FROM sqlite_sequence WHERE name='compendio_idmji';")
        conn.commit()
        existing_videos = set()
        total_existentes = 0
    else:
        existing_videos = set(row[0] for row in cur.execute("SELECT DISTINCT video_id FROM compendio_idmji WHERE video_id IS NOT NULL").fetchall())
        total_existentes = cur.execute("SELECT COUNT(*) FROM compendio_idmji").fetchone()[0]
    print(f"[+] Base de datos actual: {total_existentes} enseñanzas ({len(existing_videos)} videos registrados).")

    # Escaneo de transcripciones
    archivos_json = glob.glob(os.path.join(TRANSCRIPCIONES_DIR, "*", "*.json"))
    nuevos_archivos = [f for f in archivos_json if os.path.splitext(os.path.basename(f))[0] not in existing_videos]
    print(f"[+] Total transcripciones en disco: {len(archivos_json)} | Nuevas a procesar: {len(nuevos_archivos)}")

    if not nuevos_archivos:
        print("[✓] Todos los videos ya están indexados en SQLite local. No hay filas pendientes.")
        conn.close()
        # Verificar que Turso Cloud también esté al día
        sincronizar_turso.sincronizar_hacia_turso()
        return

    filas_canonicas = []
    filas_tematicas = []

    t0 = time.time()
    for idx, fpath in enumerate(nuevos_archivos, 1):
        if idx % 50 == 0 or idx == len(nuevos_archivos):
            print(f"  Segmentando videos nuevos [{idx}/{len(nuevos_archivos)}]...")

        try:
            with open(fpath, "r", encoding="utf-8") as jf:
                data = json.load(jf)
        except Exception:
            continue

        segs = compilar_compendio.segmentar_video(data)
        vid_id = data.get("id", "")
        base_video_url = f"https://www.youtube.com/watch?v={vid_id}"

        for s in segs:
            start_t = s["start"]
            target_url = f"{base_video_url}&t={start_t}s" if start_t > 0 else base_video_url
            titulo = s["titulo_base"]
            seccion = s["playlist"]
            transcripcion = generar_transcripcion(s["snippets"], base_video_url)

            cits = s["citations"]
            if cits:
                for cit in cits:
                    libro, cap, ver, pasaje = cit[:4]
                    end_ver = cit[4] if len(cit) > 4 else ver
                    n_lib = norm(libro)
                    if n_lib in db_books_map:
                        db_id, db_name = db_books_map[n_lib]
                        if ver > 0:
                            v_ini = ver
                            v_fin = end_ver if (end_ver and end_ver >= ver) else ver
                            cita = f"{db_name} {pasaje}"
                        else:
                            # 0 indica capítulo completo / genérico sin versículo específico
                            v_ini = 0
                            v_fin = 0
                            cita = f"{db_name} {cap}"
                        filas_canonicas.append({
                            "tipo": "CANONICO",
                            "libro_id": db_id,
                            "capitulo": cap,
                            "versiculo_inicio": v_ini,
                            "versiculo_fin": v_fin,
                            "cita": cita,
                            "titulo": titulo,
                            "video_url": target_url,
                            "video_id": vid_id,
                            "seccion": seccion,
                            "transcripcion": transcripcion,
                            "start": start_t
                        })
                    else:
                        filas_tematicas.append({
                            "tipo": "TEMATICO",
                            "libro_id": None,
                            "capitulo": None,
                            "versiculo_inicio": None,
                            "versiculo_fin": None,
                            "cita": "",
                            "titulo": titulo,
                            "video_url": target_url,
                            "video_id": vid_id,
                            "seccion": seccion,
                            "transcripcion": transcripcion,
                            "start": start_t
                        })
            else:
                filas_tematicas.append({
                    "tipo": "TEMATICO",
                    "libro_id": None,
                    "capitulo": None,
                    "versiculo_inicio": None,
                    "versiculo_fin": None,
                    "cita": "",
                    "titulo": titulo,
                    "video_url": target_url,
                    "video_id": vid_id,
                    "seccion": seccion,
                    "transcripcion": transcripcion,
                    "start": start_t
                })

    print(f"\n[+] Segmentación de nuevos videos finalizada en {time.time() - t0:.1f}s.")
    print(f"  • Nuevas canónicas por versículo: {len(filas_canonicas)}")
    print(f"  • Nuevas temáticas doctrinales:   {len(filas_tematicas)}")
    print(f"  • TOTAL nuevas intervenciones:    {len(filas_canonicas) + len(filas_tematicas)}")

    filas_canonicas.sort(key=lambda r: (r["libro_id"], r["capitulo"], r["versiculo_inicio"], r["start"]))
    filas_tematicas.sort(key=lambda r: (r["titulo"], r["start"]))

    print("\n[+] Insertando nuevas filas en SQLite (biblia_rvr1960.db)...")

    insert_sql = """
    INSERT INTO compendio_idmji (
        tipo, libro_id, capitulo, versiculo_inicio, versiculo_fin,
        cita, titulo, video_url, video_id, seccion, transcripcion
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """

    todas_las_nuevas_filas = []
    for r in filas_canonicas:
        todas_las_nuevas_filas.append((
            r["tipo"], r["libro_id"], r["capitulo"], r["versiculo_inicio"], r["versiculo_fin"],
            r["cita"], r["titulo"], r["video_url"], r["video_id"], r["seccion"], r["transcripcion"]
        ))
    for r in filas_tematicas:
        todas_las_nuevas_filas.append((
            r["tipo"], r["libro_id"], r["capitulo"], r["versiculo_inicio"], r["versiculo_fin"],
            r["cita"], r["titulo"], r["video_url"], r["video_id"], r["seccion"], r["transcripcion"]
        ))

    cur.executemany(insert_sql, todas_las_nuevas_filas)
    cur.execute("PRAGMA user_version = 3;")
    conn.commit()
    print("[+] Ejecutando VACUUM para compactar base de datos...")
    cur.execute("VACUUM;")
    conn.commit()
    conn.close()

    print(f"[✓] Se insertaron {len(todas_las_nuevas_filas)} nuevas filas en la base SQLite local.")

    try:
        sincronizar_turso.sincronizar_hacia_turso(rebuild=rebuild_all)
    except Exception as e:
        print(f"[!] Aviso: Sincronización a Turso pospuesta: {e}")

if __name__ == "__main__":
    actualizar_db()
