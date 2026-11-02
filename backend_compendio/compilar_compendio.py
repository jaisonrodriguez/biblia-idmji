#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Compilador Doctrinal Bíblico Granular para IDMJI.
- Analiza las 1.445 transcripciones completas con sus marcas de tiempo al segundo.
- Detecta y segmenta intervenciones de preguntas y respuestas de los creyentes.
- Distingue enseñanzas monológicas continuas de cultos mixtos con preguntas.
- Minería de citas bíblicas en lenguaje hablado (ej. 'mateo 922', 'marcos 10 52', 'lucas 750', 'salmo 127').
- Compilación canónica estricta (Génesis a Apocalipsis, capítulos y versículos ascendentes).
- Enlaces exactos de YouTube al segundo de cada intervención (&t=Xs).
- Texto 100% literal sin resúmenes ni paráfrasis.
"""

import os
import sys
import time
import json
import re
import glob
import sqlite3
import unicodedata

BASE_DIR = "/home/rocua/Proyectos/IDMJI/Compendio_Doctrinal_IDMJI"
# Fuente de verdad para validar que capítulo y versículo existen (RVR1960)
DB_BIBLIA = "/home/rocua/Proyectos/IDMJI/Biblia_Android/app/src/main/assets/biblia_rvr1960.db"
TRANSCRIPCIONES_DIR = os.path.join(BASE_DIR, "transcripciones")
COMPENDIOS_DIR = os.path.join(BASE_DIR, "compendios")
OUTPUT_MD = os.path.join(COMPENDIOS_DIR, "Compendio_IDMJI_Biblico_Literal.md")

LIBROS_CANONICOS = [
    # Antiguo Testamento
    "Génesis", "Éxodo", "Levítico", "Números", "Deuteronomio",
    "Josué", "Jueces", "Rut", "1 Samuel", "2 Samuel", "1 Reyes", "2 Reyes",
    "1 Crónicas", "2 Crónicas", "Esdras", "Nehemías", "Ester",
    "Job", "Salmos", "Proverbios", "Eclesiastés", "Cantares",
    "Isaías", "Jeremías", "Lamentaciones", "Ezequiel", "Daniel",
    "Oseas", "Joel", "Amós", "Abdías", "Jonás", "Miqueas", "Nahum",
    "Habacuc", "Sofonías", "Hageo", "Zacarías", "Malaquías",
    # Nuevo Testamento
    "Mateo", "Marcos", "Lucas", "Juan", "Hechos",
    "Romanos", "1 Corintios", "2 Corintios", "Gálatas", "Efesios",
    "Filipenses", "Colosenses", "1 Tesalonicenses", "2 Tesalonicenses",
    "1 Timoteo", "2 Timoteo", "Tito", "Filemón", "Hebreos",
    "Santiago", "1 Pedro", "2 Pedro", "1 Juan", "2 Juan", "3 Juan",
    "Judas", "Apocalipsis"
]

BIBLE_MAX_CHAPTERS = {
    "Génesis": 50, "Éxodo": 40, "Levítico": 27, "Números": 36, "Deuteronomio": 34,
    "Josué": 24, "Jueces": 21, "Rut": 4, "1 Samuel": 31, "2 Samuel": 24,
    "1 Reyes": 22, "2 Reyes": 25, "1 Crónicas": 29, "2 Crónicas": 36, "Esdras": 10,
    "Nehemías": 13, "Ester": 10, "Job": 42, "Salmos": 150, "Proverbios": 31,
    "Eclesiastés": 12, "Cantares": 8, "Isaías": 66, "Jeremías": 52, "Lamentaciones": 5,
    "Ezequiel": 48, "Daniel": 12, "Oseas": 14, "Joel": 3, "Amós": 9,
    "Abdías": 1, "Jonás": 4, "Miqueas": 7, "Nahum": 3, "Habacuc": 3,
    "Sofonías": 3, "Hageo": 2, "Zacarías": 14, "Malaquías": 4,
    "Mateo": 28, "Marcos": 16, "Lucas": 24, "Juan": 21, "Hechos": 28,
    "Romanos": 16, "1 Corintios": 16, "2 Corintios": 13, "Gálatas": 6, "Efesios": 6,
    "Filipenses": 4, "Colosenses": 4, "1 Tesalonicenses": 5, "2 Tesalonicenses": 3,
    "1 Timoteo": 6, "2 Timoteo": 4, "Tito": 3, "Filemón": 1, "Hebreos": 13,
    "Santiago": 5, "1 Pedro": 5, "2 Pedro": 3, "1 Juan": 5, "2 Juan": 1,
    "3 Juan": 1, "Judas": 1, "Apocalipsis": 22
}

BOOK_ALIASES = {
    "1 corintios": "1 Corintios", "1ª corintios": "1 Corintios", "1a corintios": "1 Corintios", "primera de corintios": "1 Corintios", "1ª de corintios": "1 Corintios", "1a de corintios": "1 Corintios",
    "2 corintios": "2 Corintios", "2ª corintios": "2 Corintios", "2a corintios": "2 Corintios", "segunda de corintios": "2 Corintios", "2ª de corintios": "2 Corintios",
    "1 pedro": "1 Pedro", "1ª pedro": "1 Pedro", "primera de pedro": "1 Pedro", "1ª de pedro": "1 Pedro",
    "2 pedro": "2 Pedro", "2ª pedro": "2 Pedro", "segunda de pedro": "2 Pedro", "2ª de pedro": "2 Pedro",
    "1 juan": "1 Juan", "1ª juan": "1 Juan", "primera de juan": "1 Juan", "2 juan": "2 Juan", "3 juan": "3 Juan",
    "1 tesalonicenses": "1 Tesalonicenses", "1ª tesalonicenses": "1 Tesalonicenses", "primera de tesalonicenses": "1 Tesalonicenses",
    "2 tesalonicenses": "2 Tesalonicenses", "2ª tesalonicenses": "2 Tesalonicenses", "segunda de tesalonicenses": "2 Tesalonicenses",
    "1 timoteo": "1 Timoteo", "1ª timoteo": "1 Timoteo", "primera de timoteo": "1 Timoteo", "1ª de timoteo": "1 Timoteo",
    "2 timoteo": "2 Timoteo", "2ª timoteo": "2 Timoteo", "segunda de timoteo": "2 Timoteo", "2ª de timoteo": "2 Timoteo",
    "1 samuel": "1 Samuel", "1ª samuel": "1 Samuel", "primera de samuel": "1 Samuel", "2 samuel": "2 Samuel", "2ª samuel": "2 Samuel",
    "1 reyes": "1 Reyes", "1ª reyes": "1 Reyes", "primera de reyes": "1 Reyes", "2 reyes": "2 Reyes", "2ª reyes": "2 Reyes",
    "1 cronicas": "1 Crónicas", "1 crónicas": "1 Crónicas", "2 cronicas": "2 Crónicas", "2 crónicas": "2 Crónicas",
    "san mateo": "Mateo", "mateo": "Mateo",
    "san marcos": "Marcos", "marcos": "Marcos",
    "san lucas": "Lucas", "lucas": "Lucas",
    "san juan": "Juan", "juan": "Juan",
    "hechos de los apostoles": "Hechos", "hechos de los apóstoles": "Hechos", "hechos": "Hechos",
    "romanos": "Romanos", "galatas": "Gálatas", "gálatas": "Gálatas", "efesios": "Efesios",
    "filipenses": "Filipenses", "colosenses": "Colosenses", "tito": "Tito", "filemon": "Filemón", "filemón": "Filemón",
    "hebreos": "Hebreos", "santiago": "Santiago", "judas": "Judas", "apocalipsis": "Apocalipsis",
    "genesis": "Génesis", "génesis": "Génesis", "exodo": "Éxodo", "éxodo": "Éxodo",
    "levitico": "Levítico", "levítico": "Levítico", "numeros": "Números", "números": "Números",
    "deuteronomio": "Deuteronomio", "josue": "Josué", "josué": "Josué", "jueces": "Jueces", "rut": "Rut",
    "esdras": "Esdras", "nehemias": "Nehemías", "nehemías": "Nehemías", "ester": "Ester",
    "job": "Job", "salmo": "Salmos", "salmos": "Salmos", "proverbios": "Proverbios",
    "eclesiastes": "Eclesiastés", "eclesiastés": "Eclesiastés", "cantares": "Cantares",
    "isaias": "Isaías", "isaías": "Isaías", "jeremias": "Jeremías", "jeremías": "Jeremías",
    "lamentaciones": "Lamentaciones", "ezequiel": "Ezequiel", "daniel": "Daniel",
    "oseas": "Oseas", "joel": "Joel", "amos": "Amós", "amós": "Amós", "abdias": "Abdías", "abdías": "Abdías",
    "jonas": "Jonás", "jonás": "Jonás", "miqueas": "Miqueas", "nahum": "Nahum",
    "habacuc": "Habacuc", "sofonias": "Sofonías", "sofonías": "Sofonías", "hageo": "Hageo",
    "zacarias": "Zacarías", "zacarías": "Zacarías", "malaquias": "Malaquías", "malaquías": "Malaquías"
}

NEW_Q_TRIGGERS = [
    r'\b(?:buenas\s+noches|buenas\s+tardes|buenos\s+d[ií]as)\s+hermana\b',
    r'\bhermana\s+(?:mar[ií]a\s+luisa|buenas\s+noches|buenas\s+tardes|buenos\s+d[ií]as)\b',
    r'\bgracias\s+por\s+(?:darme\s+la\s+palabra|la\s+oportunidad|permitirme)\b',
    r'\bhermana\s+quer[ií]a\s+hacerle\s+una\s+pregunta\b',
    r'\b(?:siguiente|otra)\s+pregunta\b',
    r'\b(?:a\s+ver|el\s+hermano|la\s+hermana)\s+(?:all[aá]\s+atr[aá]s|del\s+micr[oó]fono)\b',
    r'\b(?:tengo|traigo)\s+una\s+pregunta\b',
    r'\b(?:mi|la)\s+pregunta\s+(?:es|est[aá]|de\s+la\s+biblia)\b',
    r'\b(?:quer[ií]a|quiero|quisiera)\s+hacerle\s+una\s+pregunta\b'
]

# Un disparador solo abre un bloque "Pregunta" si en los siguientes Q_VENTANA_SEG
# segundos aparece un indicio explícito de pregunta (evita oraciones, saludos y coros).
Q_VENTANA_SEG = 90
Q_INDICIO = re.compile(
    r'\bpregunt|\bquisiera\s+saber\b|\bquiero\s+saber\b|\bquer[ií]a\s+saber\b|'
    r'\bme\s+gustar[ií]a\s+saber\b|\b(?:mi|una|la)\s+duda\b|\binquietud\b|\bme\s+(?:explique|explica)\b|'
    r'\b(?:me|nos)\s+podr[ií]a\s+(?:explicar|enseñar|aclarar|decir)\b'
)
# "hermana maría luisa" en tercera persona ("nuestra hermana maría luisa") no es una intervención
Q_TERCERA_PERSONA = re.compile(r'\b(?:nuestra|la|amada|querida|de|a|por|con|y|que|maestra|apóstol)\s+$')

_MAX_VERSOS = None

def _norm(s):
    return unicodedata.normalize("NFD", s).encode("ascii", "ignore").decode("utf-8").lower().strip()

def max_versiculo(libro, cap):
    """Último versículo del capítulo según RVR1960 (0 si el capítulo no existe)."""
    global _MAX_VERSOS
    if _MAX_VERSOS is None:
        if not os.path.exists(DB_BIBLIA):
            raise RuntimeError("No se encontró la BD bíblica para validar citas: %s" % DB_BIBLIA)
        con = sqlite3.connect(DB_BIBLIA)
        try:
            filas = con.execute(
                "SELECT l.nombre, v.capitulo, MAX(v.versiculo) FROM versiculos v "
                "JOIN libros l ON l.id = v.libro_id GROUP BY v.libro_id, v.capitulo").fetchall()
        finally:
            con.close()
        _MAX_VERSOS = {(_norm(n), c): m for n, c, m in filas}
    return _MAX_VERSOS.get((_norm(libro), cap), 0)

def cita_valida(libro, cap, ver):
    """True si el capítulo existe y el versículo (0 = capítulo completo) existe en él."""
    m = max_versiculo(libro, cap)
    return m > 0 and 0 <= ver <= m

def _fin_valido(libro, cap, ver, end_v):
    if end_v and ver < int(end_v) <= max_versiculo(libro, cap):
        return end_v
    return None

UNIDADES_ESP = {
    "cero": 0, "un": 1, "uno": 1, "una": 1, "primero": 1, "primera": 1,
    "dos": 2, "segundo": 2, "segunda": 2, "tres": 3, "tercero": 3,
    "cuatro": 4, "cinco": 5, "seis": 6, "siete": 7, "ocho": 8, "nueve": 9,
    "diez": 10, "once": 11, "doce": 12, "trece": 13, "catorce": 14, "quince": 15,
    "dieciseis": 16, "dieciséis": 16, "diecisiete": 17, "dieciocho": 18, "diecinueve": 19,
    "veinte": 20, "veintiuno": 21, "veintidos": 22, "veintidós": 22, "veintitres": 23, "veintitrés": 23,
    "veinticuatro": 24, "veinticinco": 25, "veintiseis": 26, "veintiséis": 26, "veintisiete": 27,
    "veintiocho": 28, "veintinueve": 29
}
DECENAS_ESP = {
    "treinta": 30, "cuarenta": 40, "cincuenta": 50, "sesenta": 60,
    "setenta": 70, "ochenta": 80, "noventa": 90, "cien": 100, "ciento": 100
}

def parse_num_esp(val):
    if not val:
        return None
    val = val.strip().lower()
    if val.isdigit():
        return int(val)
    if val in UNIDADES_ESP:
        return UNIDADES_ESP[val]
    if val in DECENAS_ESP:
        return DECENAS_ESP[val]
    parts = val.split(" y ")
    if len(parts) == 2 and parts[0] in DECENAS_ESP and parts[1] in UNIDADES_ESP:
        return DECENAS_ESP[parts[0]] + UNIDADES_ESP[parts[1]]
    return None

NUM_WORD_PAT = r"(?:\d+|uno|un|primero|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|diez|once|doce|trece|catorce|quince|diecis[eé]is|diecisiete|dieciocho|diecinueve|veinte|veintiuno|veintid[oó]s|veintitr[eé]s|veinticuatro|veinticinco|veintis[eé]is|veintisiete|veintiocho|veintinueve|treinta(?:\s+y\s+(?:uno|dos|tres|cuatro|cinco|seis|siete|ocho|nueve))?)"

BRIDGE_RE = re.compile(
    r"^\s*(?:cap[ií]tulo\s*)?(\d+)\b(?:.{0,60}?)\b(?:vers[ií]culos?|versos?|vrs?)\b(?:\s*(?:n[uú]mero|del|al|a|en\s*el))?\s*(" + NUM_WORD_PAT + r")(?:\s*(?:al|a|hasta(?:\s*el)?|\-)\s*(" + NUM_WORD_PAT + r"))?",
    re.IGNORECASE | re.DOTALL
)

def extract_citations_from_text(text):
    """Extrae referencias bíblicas tanto de formato escrito como de números hablados corridos."""
    text_lower = text.lower()
    citations = []
    
    for alias in sorted(BOOK_ALIASES.keys(), key=len, reverse=True):
        pattern = r'\b' + re.escape(alias) + r'\b'
        for m in re.finditer(pattern, text_lower):
            book = BOOK_ALIASES[alias]
            max_c = BIBLE_MAX_CHAPTERS.get(book, 150)
            tail = text_lower[m.end():m.end()+100]

            # 1. Puente hablado: 'génesis capítulo 1 quiero pasar al versículo número 26', 'génesis capítulo 1 del versículo uno al cuatro'
            m_br = BRIDGE_RE.search(tail)
            if m_br:
                c = int(m_br.group(1))
                v = parse_num_esp(m_br.group(2))
                end_v = parse_num_esp(m_br.group(3))
                if v and c <= max_c and cita_valida(book, c, v):
                    if end_v and v < end_v <= max_versiculo(book, c):
                        pasaje = f"{c}:{v}-{end_v}"
                        citations.append((book, c, v, pasaje, end_v))
                    else:
                        pasaje = f"{c}:{v}"
                        citations.append((book, c, v, pasaje, v))
                    continue

            # 2. Clásico: 'mateo 5 versículo 44', 'mateo 9:22', 'mateo 5 vr 44 al 46'
            m_cv = re.search(r'^\s*(?:cap[ií]tulo\s*)?(\d+)\s*(?:[\:\,\.]|vers[ií]culos?|versos?|vrs?)\.?\s*(\d+)(?:\s*(?:al|\-)\s*(\d+))?', tail)
            if m_cv:
                c = int(m_cv.group(1))
                v = int(m_cv.group(2))
                if c <= max_c and v > 0 and cita_valida(book, c, v):
                    end_v = _fin_valido(book, c, v, m_cv.group(3))
                    pasaje = f"{c}:{v}-{end_v}" if end_v else f"{c}:{v}"
                    citations.append((book, c, v, pasaje, int(end_v) if end_v else v))
                    continue

            # 3. Dos números corridos: 'marcos 10 52', 'mateo 9 22'
            m_two_nums = re.search(r'^\s*(\d{1,2})\s+(\d{1,3})\b(?:\s*(?:al|\-)\s*(\d+))?', tail)
            if m_two_nums:
                c = int(m_two_nums.group(1))
                v = int(m_two_nums.group(2))
                if c <= max_c and v > 0 and cita_valida(book, c, v):
                    end_v = _fin_valido(book, c, v, m_two_nums.group(3))
                    pasaje = f"{c}:{v}-{end_v}" if end_v else f"{c}:{v}"
                    citations.append((book, c, v, pasaje, int(end_v) if end_v else v))
                    continue

            # 4. Concatenados hablados: 'mateo 922', 'lucas 750', 'lucas 1842', 'salmo 127', 'salmo 1081'
            m_concat = re.search(r'^\s*(\d{3,4})\b', tail)
            if m_concat:
                num_str = m_concat.group(1)
                val = int(num_str)
                if book == "Salmos" and cita_valida(book, val, 0):
                    citations.append((book, val, 0, f"{val}", 0))
                    continue
                encontrada = None
                for k in ((2, 1) if len(num_str) == 3 else (2, 1, 3)):
                    if num_str[k] == "0":
                        continue
                    c, v = int(num_str[:k]), int(num_str[k:])
                    if c <= max_c and cita_valida(book, c, v):
                        encontrada = (book, c, v, f"{c}:{v}", v)
                        break
                if encontrada:
                    citations.append(encontrada)
                    continue

            # 5. Solo capítulo: 'hechos 2', 'juan 17', 'salmo 23'
            m_cap_only = re.search(r'^\s*(?:cap[ií]tulo\s*)?(\d{1,3})\b', tail)
            if m_cap_only:
                c = int(m_cap_only.group(1))
                if c <= max_c and cita_valida(book, c, 0):
                    citations.append((book, c, 0, f"{c}", 0))
                    continue

    # Deduplicar preservando orden
    unique = []
    seen = set()
    for c in citations:
        key = (c[0], c[1], c[2])
        if key not in seen:
            seen.add(key)
            unique.append(c)
    return unique

def parse_referencia_biblica(titulo):
    """Extrae libro, capítulo y versículo del título de un video si existe."""
    cits = extract_citations_from_text(titulo)
    if cits:
        return cits[0]
    return "Enseñanzas Doctrinales y Temáticas", 9999, 9999, ""

def formatear_tiempo(segundos):
    seg = int(segundos)
    m, s = divmod(seg, 60)
    h, m = divmod(m, 60)
    if h > 0:
        return f"{h:02d}:{m:02d}:{s:02d}"
    return f"{m:02d}:{s:02d}"

def limpiar_titulo_tema(titulo):
    t = titulo.replace("#IDMJI", "").replace("#idmji", "").strip(" -–,")
    t = re.sub(r'\s*-\s*Hna\.?\s*María Luisa.*$', '', t, flags=re.IGNORECASE).strip(" -–,")
    return t if t else titulo

def segmentar_video(data):
    """
    Segmenta un video en unidades doctrinales coherentes:
    - Si es monólogo continuo: 1 solo bloque expositivo.
    - Si es mixto/preguntas: Exposición principal + Bloques individuales de cada pregunta y respuesta.
    """
    # Algunos JSON de YouTube traen los snippets desordenados: se ordenan por tiempo (orden estable,
    # el texto de cada snippet no se toca).
    snippets = sorted(data.get("snippets", []), key=lambda s: s["start"])
    if not snippets:
        return []

    title = data.get("titulo", "")
    url = data.get("url", "")
    vid_id = data.get("id", "")
    playlist = data.get("playlist", "")

    # 1. Detección de puntos de corte. Los cortes se conservan (granularidad de citas con su minuto),
    #    pero un bloque solo se etiqueta "Pregunta" si es una intervención en segunda persona
    #    con indicio explícito de pregunta en los siguientes Q_VENTANA_SEG segundos.
    q_indices = []
    es_pregunta = []
    textos = [s["text"].lower() for s in snippets]
    for idx, s in enumerate(snippets):
        txt = textos[idx]
        for p in NEW_Q_TRIGGERS:
            m = re.search(p, txt)
            if not m:
                continue
            previo = (textos[idx - 1] if idx > 0 else "") + " " + txt[:m.start()]
            tercera_persona = "mar" in m.group(0) and Q_TERCERA_PERSONA.search(previo)
            limite = s["start"] + Q_VENTANA_SEG
            j = idx
            ventana = []
            while j < len(snippets) and snippets[j]["start"] <= limite:
                ventana.append(textos[j])
                j += 1
            valida = (not tercera_persona) and bool(Q_INDICIO.search(" ".join(ventana)))
            # Evitar duplicar marcadores muy cercanos (< 45s) en una misma intervención
            if not q_indices or (int(s["start"]) - int(snippets[q_indices[-1]]["start"])) > 45:
                q_indices.append(idx)
                es_pregunta.append(valida)
            elif valida:
                es_pregunta[-1] = True
            break

    segments = []

    # CASO A: Monólogo Continuo / Enseñanza sin preguntas
    if not q_indices:
        title_cits = extract_citations_from_text(title)
        if not title_cits:
            early_text = " ".join([s["text"] for s in snippets[:60]])
            title_cits = extract_citations_from_text(early_text)
        
        segments.append({
            "id": vid_id,
            "url": url,
            "playlist": playlist,
            "titulo_base": title,
            "tipo": "ENSEÑANZA_CONTINUA",
            "start": int(snippets[0]["start"]),
            "end": int(snippets[-1]["start"]),
            "citations": title_cits[:2] if title_cits else [],
            "snippets": snippets
        })
        return segments

    # CASO B: Culto Mixto o Sesión de Preguntas
    # Si la primera pregunta inicia después del minuto 8, la apertura es la Exposición Central
    if q_indices and int(snippets[q_indices[0]]["start"]) > 480:
        expo_snips = snippets[:q_indices[0]]
        expo_text = " ".join([s["text"] for s in expo_snips])
        title_cits = extract_citations_from_text(title)
        if not title_cits:
            title_cits = extract_citations_from_text(expo_text[:5000])

        segments.append({
            "id": vid_id,
            "url": url,
            "playlist": playlist,
            "titulo_base": f"Exposición Central: {title}",
            "tipo": "EXPOSICION_CENTRAL",
            "start": int(expo_snips[0]["start"]),
            "end": int(expo_snips[-1]["start"]),
            "citations": title_cits[:2] if title_cits else [],
            "snippets": expo_snips
        })

    # Extraer cada bloque de Pregunta y Respuesta
    for i, qi in enumerate(q_indices):
        end_idx = q_indices[i+1] if i + 1 < len(q_indices) else len(snippets)
        seg_snips = snippets[qi:end_idx]
        seg_text = " ".join([s["text"] for s in seg_snips])
        cits = extract_citations_from_text(seg_text)

        # Si hay versículos específicos de un capítulo, no duplicar la cita genérica del capítulo solo
        v_set = set((c[0], c[1]) for c in cits if c[2] > 0)
        filtered_cits = [c for c in cits if not (c[2] == 0 and (c[0], c[1]) in v_set)]

        # Resumen limpio de la pregunta para el subtítulo
        q_preview = " ".join([s["text"] for s in seg_snips[:4]])
        q_preview = re.sub(r'^(?:buenas\s+noches|buenas\s+tardes|buenos\s+d[ií]as|hermana|mar[ií]a\s+luisa|gracias|por\s+darme\s+la\s+palabra|\s+|,|\.)+', '', q_preview, flags=re.I).strip()
        q_preview = q_preview[:95].strip(" -,.")

        if es_pregunta[i]:
            sub_titulo = f"Pregunta: {q_preview} — ({title[:40]})" if q_preview else f"Pregunta y Respuesta — {title[:40]}"
            tipo_seg = "PREGUNTA_RESPUESTA"
        else:
            sub_titulo = f"Fragmento {formatear_tiempo(seg_snips[0]['start'])} — {title}"
            tipo_seg = "FRAGMENTO"

        segments.append({
            "id": vid_id,
            "url": url,
            "playlist": playlist,
            "titulo_base": sub_titulo,
            "tipo": tipo_seg,
            "start": int(seg_snips[0]["start"]),
            "end": int(seg_snips[-1]["start"]),
            "citations": filtered_cits,
            "snippets": seg_snips
        })

    return segments

def escribir_bloques_transcripcion(out, snippets, base_url):
    """Escribe los fragmentos literales en bloques legibles de ~35 segundos con enlaces exactos."""
    bloque_texto = []
    tiempo_inicio = None

    for s in snippets:
        if tiempo_inicio is None:
            tiempo_inicio = int(s["start"])
        bloque_texto.append(s["text"].strip())

        if len(bloque_texto) >= 6 or (int(s["start"]) - tiempo_inicio) >= 35:
            t_str = formatear_tiempo(tiempo_inicio)
            url_t = f"{base_url}&t={tiempo_inicio}s"
            parrafo = " ".join(bloque_texto)
            out.write(f"[[{t_str}]]({url_t}) {parrafo}\n\n")
            bloque_texto = []
            tiempo_inicio = None

    if bloque_texto:
        t_str = formatear_tiempo(tiempo_inicio or 0)
        url_t = f"{base_url}&t={tiempo_inicio or 0}s"
        parrafo = " ".join(bloque_texto)
        out.write(f"[[{t_str}]]({url_t}) {parrafo}\n\n")

def compilar_compendio():
    print("===========================================================")
    print("INICIANDO COMPILACIÓN CANÓNICA GRANULAR IDMJI")
    print("===========================================================")
    os.makedirs(COMPENDIOS_DIR, exist_ok=True)

    entradas_por_libro = {l: [] for l in LIBROS_CANONICOS}
    tematicas = []

    archivos_json = glob.glob(os.path.join(TRANSCRIPCIONES_DIR, "*", "*.json"))
    total_videos = len(archivos_json)
    print(f"Total de videos a procesar: {total_videos}")

    total_intervenciones = 0

    for idx, fpath in enumerate(archivos_json, 1):
        if idx % 200 == 0 or idx == total_videos:
            print(f"  Procesando video [{idx}/{total_videos}]...")

        try:
            with open(fpath, "r", encoding="utf-8") as jf:
                data = json.load(jf)
        except Exception:
            continue

        segs = segmentar_video(data)

        for s in segs:
            total_intervenciones += 1
            cits = s["citations"]

            if cits:
                for cit in cits:
                    libro, cap, ver, pasaje = cit[:4]
                    end_ver = cit[4] if len(cit) > 4 else ver
                    reg = {
                        "id": s["id"],
                        "url": s["url"],
                        "playlist": s["playlist"],
                        "titulo_base": s["titulo_base"],
                        "tipo": s["tipo"],
                        "start": s["start"],
                        "end": s["end"],
                        "libro": libro,
                        "cap": cap,
                        "ver": ver,
                        "pasaje": pasaje,
                        "snippets": s["snippets"]
                    }
                    if libro in LIBROS_CANONICOS:
                        entradas_por_libro[libro].append(reg)
                    else:
                        tematicas.append(reg)
            else:
                reg = {
                    "id": s["id"],
                    "url": s["url"],
                    "playlist": s["playlist"],
                    "titulo_base": s["titulo_base"],
                    "tipo": s["tipo"],
                    "start": s["start"],
                    "end": s["end"],
                    "libro": "Enseñanzas Doctrinales y Temáticas",
                    "cap": 9999,
                    "ver": 9999,
                    "pasaje": "",
                    "snippets": s["snippets"]
                }
                tematicas.append(reg)

    print(f"\n[+] Total de intervenciones doctrinales generadas: {total_intervenciones}")

    # ORDENAMIENTO CANÓNICO Y ASCENDENTE ESTRICTO
    for l in LIBROS_CANONICOS:
        entradas_por_libro[l].sort(key=lambda x: (x["cap"], x["ver"], x["start"]))

    tematicas.sort(key=lambda x: x["titulo_base"])

    print(f"Generando documento Markdown maestro en: {OUTPUT_MD}...")

    with open(OUTPUT_MD, "w", encoding="utf-8") as out:
        out.write("# Compendio Doctrinal IDMJI — Orden Canónico de la Biblia\n\n")
        out.write("> **Documento Oficial de Estudio:** Transcripciones 100% literales palabra por palabra desde las transmisiones y videos oficiales de la Iglesia de Dios Ministerial de Jesucristo Internacional. Indexado granularmente por cada intervención, pregunta y respuesta de los creyentes a la Hna. María Luisa Piraquive. Ordenado estrictamente de Génesis a Apocalipsis por Libros, Capítulos y Versículos, con enlaces al segundo exacto.\n\n")
        out.write(f"* **Total de Videos Procesados:** `{total_videos}`\n")
        out.write(f"* **Total de Intervenciones Doctrinales Indexadas:** `{total_intervenciones}`\n\n")
        out.write("---\n\n")

        # Índice canónico
        out.write("## 📑 Índice General por Libros Bíblicos\n\n")
        libros_con_contenido = [l for l in LIBROS_CANONICOS if len(entradas_por_libro[l]) > 0]

        columnas = []
        for l in libros_con_contenido:
            cant = len(entradas_por_libro[l])
            anchor = l.lower().replace(" ", "-").replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
            columnas.append(f"* [📖 **{l}** ({cant} intervenciones)](#-{anchor})")

        if tematicas:
            columnas.append(f"* [🕊️ **Enseñanzas Doctrinales y Temáticas** ({len(tematicas)} intervenciones)](#-ensenanzas-doctrinales-y-tematicas)")

        out.write("\n".join(columnas) + "\n\n")
        out.write("---\n\n")

        # Contenido Bíblico Canónico
        for libro in libros_con_contenido:
            intervenciones = entradas_por_libro[libro]
            out.write(f"\n# 📖 {libro}\n\n")

            for item in intervenciones:
                ref_header = f"{libro} {item['pasaje']}" if item["pasaje"] else libro
                titulo_limpio = limpiar_titulo_tema(item["titulo_base"])
                t_str_inicio = formatear_tiempo(item["start"])
                url_exacta = f"{item['url']}&t={item['start']}s"

                out.write(f"## 📜 {ref_header} — {titulo_limpio}\n\n")
                out.write(f"* **Momento Exacto:** [[{t_str_inicio} en YouTube]]({url_exacta})\n")
                out.write(f"* **Tipo de Intervención:** `{item['tipo']}` | **Sección:** `{item['playlist']}` | **ID:** `{item['id']}`\n\n")
                out.write("### Transcripción Literal Oficial:\n\n")

                escribir_bloques_transcripcion(out, item["snippets"], item["url"])
                out.write("\n---\n\n")

        # Sección Temática
        if tematicas:
            out.write("\n# 🕊️ Enseñanzas Doctrinales y Temáticas\n\n")
            for item in tematicas:
                titulo_limpio = limpiar_titulo_tema(item["titulo_base"])
                t_str_inicio = formatear_tiempo(item["start"])
                url_exacta = f"{item['url']}&t={item['start']}s"

                out.write(f"## 🕊️ {titulo_limpio}\n\n")
                out.write(f"* **Momento Exacto:** [[{t_str_inicio} en YouTube]]({url_exacta})\n")
                out.write(f"* **Tipo de Intervención:** `{item['tipo']}` | **Sección:** `{item['playlist']}` | **ID:** `{item['id']}`\n\n")
                out.write("### Transcripción Literal Oficial:\n\n")

                escribir_bloques_transcripcion(out, item["snippets"], item["url"])
                out.write("\n---\n\n")

    file_size_mb = os.path.getsize(OUTPUT_MD) / (1024 * 1024)
    print(f"\n🎉 ¡COMPILACIÓN COMPLETADA CON ÉXITO!")
    print(f"Archivo generado: {OUTPUT_MD} ({file_size_mb:.2f} MB)")

if __name__ == "__main__":
    compilar_compendio()
