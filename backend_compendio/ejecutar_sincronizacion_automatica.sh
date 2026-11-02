#!/usr/bin/env bash
# ==============================================================================
# ejecutar_sincronizacion_automatica.sh
# Automatización IDMJI:
# 1. Búsqueda de nuevos videos oficiales en YouTube (yt-dlp).
# 2. Descarga de transcripciones oficiales con timestamps.
# 3. Extracción de citas bíblicas y temas doctrinales.
# 4. Inserción incremental en SQLite local.
# 5. Sincronización instantánea con Turso Cloud (libsql://biblia-idmji-jaisonrodriguez).
# ==============================================================================

DIR="/home/rocua/Proyectos/IDMJI/Compendio_Doctrinal_IDMJI"
LOCK_FILE="/tmp/idmji_sync.lock"
LOG_FILE="$DIR/actualizacion_automatica.log"

export PATH="/home/rocua/.local/bin:/usr/local/bin:/usr/bin:/bin:$PATH"

# Prevenir ejecuciones simultáneas
exec 200>"$LOCK_FILE"
flock -n 200 || {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [!] Ya hay una sincronización en curso. Saliendo." >> "$LOG_FILE"
    exit 0
}

cd "$DIR" || exit 1

echo "===========================================================" >> "$LOG_FILE"
echo "[$(date '+%Y-%m-%d %H:%M:%S')] INICIO DE PROCESO AUTOMATIZADO IDMJI" >> "$LOG_FILE"
echo "===========================================================" >> "$LOG_FILE"

python3 "$DIR/actualizar_compendio_diario.py" >> "$LOG_FILE" 2>&1

echo "[$(date '+%Y-%m-%d %H:%M:%S')] [✓] Proceso de sincronización finalizado." >> "$LOG_FILE"
