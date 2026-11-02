#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
sincronizar_turso.py
Sincroniza las filas de compendio_idmji desde la base de datos local SQLite
hacia Turso Cloud (libsql://biblia-idmji-jaisonrodriguez.aws-sa-east-1.turso.io).
"""

import os
import sys
import sqlite3
import requests
import json
import time

TURSO_URL = "https://biblia-idmji-jaisonrodriguez.aws-sa-east-1.turso.io/v2/pipeline"
TURSO_TOKEN = "eyJhbGciOiJFZERTQSIsInR5cCI6IkpXVCJ9.eyJhIjoicnciLCJpYXQiOjE3OTM2NDcxNDIsImlkIjoiMDFhMTlkYjItNWUwMS03MWI1LWIyZWYtN2U2MDEwZjdkMmU4Iiwia2lkIjoiWk8xNF9kUElKXzFCQTVRSERrVUF3T19QVjhUZ2M3TEhMMmFFNmNZUGlncyIsInJpZCI6ImEyNzU4YTk1LTBlODgtNDk5Mi05YThkLWUzNWEzMjRhMWE5ZiJ9.oNBIyZ99PN8Mz8-peTka86_NbymbcRmqXkCVcT8-4ecXBNvF0ORtVhEQ63JCjx0yapmkXkHZo8MxSddlMANwCA"
DB_PATH = "/home/rocua/Proyectos/IDMJI/Biblia_Android/app/src/main/assets/biblia_rvr1960.db"

def ejecutar_sql_turso(sql, args=None):
    stmt = {"sql": sql}
    if args:
        stmt["args"] = [{"type": "text" if isinstance(a, str) else "integer" if isinstance(a, int) else "null", "value": str(a) if a is not None else None} for a in args]

    payload = {
        "requests": [
            {"type": "execute", "stmt": stmt}
        ]
    }
    headers = {
        "Authorization": f"Bearer {TURSO_TOKEN}",
        "Content-Type": "application/json"
    }
    r = requests.post(TURSO_URL, json=payload, headers=headers, timeout=25)
    r.raise_for_status()
    data = r.json()
    first_res = data.get("results", [{}])[0]
    if first_res.get("type") == "error":
        raise RuntimeError(f"Error de Turso: {first_res.get('error')}")
    return first_res.get("response", {}).get("result", {})

def obtener_max_id_turso():
    res = ejecutar_sql_turso("SELECT COALESCE(MAX(id), 0) FROM compendio_idmji")
    rows = res.get("rows", [])
    if rows and rows[0]:
        val = rows[0][0].get("value")
        return int(val) if val is not None else 0
    return 0

def obtener_total_turso():
    res = ejecutar_sql_turso("SELECT COUNT(*) FROM compendio_idmji")
    rows = res.get("rows", [])
    if rows and rows[0]:
        val = rows[0][0].get("value")
        return int(val) if val is not None else 0
    return 0

def subir_lote(lote_filas):
    """
    Sube un lote de filas a Turso Cloud en una sola petición HTTP atómica.
    """
    requests_list = []
    for r in lote_filas:
        # id, tipo, libro_id, capitulo, versiculo_inicio, versiculo_fin, cita, titulo, video_url, video_id, seccion, transcripcion
        args_payload = []
        for val in r:
            if val is None:
                args_payload.append({"type": "null"})
            elif isinstance(val, int):
                args_payload.append({"type": "integer", "value": str(val)})
            else:
                args_payload.append({"type": "text", "value": str(val)})

        requests_list.append({
            "type": "execute",
            "stmt": {
                "sql": """
                    INSERT OR REPLACE INTO compendio_idmji 
                    (id, tipo, libro_id, capitulo, versiculo_inicio, versiculo_fin, cita, titulo, video_url, video_id, seccion, transcripcion)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.strip(),
                "args": args_payload
            }
        })

    payload = {"requests": requests_list}
    headers = {
        "Authorization": f"Bearer {TURSO_TOKEN}",
        "Content-Type": "application/json"
    }
    r = requests.post(TURSO_URL, json=payload, headers=headers, timeout=45)
    r.raise_for_status()
    data = r.json()
    for res in data.get("results", []):
        if res.get("type") == "error":
            raise RuntimeError(f"Error en lote Turso: {res.get('error')}")

def sincronizar_hacia_turso(rebuild=False):
    print("=" * 60)
    print("SINCRONIZADOR DE DOCTRINA IDMJI HACIA TURSO CLOUD")
    print("=" * 60)

    if not os.path.exists(DB_PATH):
        print(f"[!] Error: No se encuentra {DB_PATH}")
        return False

    is_rebuild = rebuild or ("--rebuild" in sys.argv or "--rebuild-all" in sys.argv)
    if is_rebuild:
        print("[!] Modo Reconstrucción: Vaciando tabla compendio_idmji en Turso Cloud...")
        try:
            ejecutar_sql_turso("DELETE FROM compendio_idmji")
            print("[✓] Tabla en Turso Cloud vaciada exitosamente.")
        except Exception as e:
            print(f"[!] Aviso al vaciar Turso: {e}")

    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()

    local_max = cur.execute("SELECT COALESCE(MAX(id), 0) FROM compendio_idmji").fetchone()[0]
    local_count = cur.execute("SELECT COUNT(*) FROM compendio_idmji").fetchone()[0]
    print(f"[+] Base local SQLite: Total={local_count}, Max ID={local_max}")

    turso_max = obtener_max_id_turso()
    turso_count = obtener_total_turso()
    print(f"[+] Base Turso Cloud:  Total={turso_count}, Max ID={turso_max}")

    if local_max <= turso_max and local_count <= turso_count:
        print("[✓] Turso Cloud ya está 100% al día con la base local. No se requieren inserciones.")
        conn.close()
        return True

    print(f"[+] Se detectaron nuevas enseñanzas locales (IDs > {turso_max}). Subiendo...")
    filas_nuevas = cur.execute("""
        SELECT id, tipo, libro_id, capitulo, versiculo_inicio, versiculo_fin,
               cita, titulo, video_url, video_id, seccion, transcripcion
        FROM compendio_idmji
        WHERE id > ?
        ORDER BY id ASC
    """, (turso_max,)).fetchall()

    total_nuevas = len(filas_nuevas)
    print(f"[+] Total de filas a subir a Turso: {total_nuevas}")

    batch_size = 50
    subidas = 0
    t0 = time.time()

    for i in range(0, total_nuevas, batch_size):
        lote = filas_nuevas[i:i + batch_size]
        subir_lote(lote)
        subidas += len(lote)
        print(f"  Subiendo a Turso Cloud: [{subidas}/{total_nuevas}]...")

    elapsed = time.time() - t0
    turso_final_count = obtener_total_turso()
    print(f"\n[✓] ¡Sincronización con Turso Cloud completada con éxito en {elapsed:.1f}s!")
    print(f"    Total en Turso ahora: {turso_final_count} enseñanzas.")
    conn.close()
    return True

if __name__ == "__main__":
    sincronizar_hacia_turso()
