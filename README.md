# BRIEFING TÉCNICO Y ESTADO DEL PROYECTO: APP BIBLIA IDMJI

## 1. Identificación y Propósito del Proyecto
* **Nombre de la Aplicación:** Biblia IDMJI (Iglesia de Dios Ministerial de Jesucristo Internacional).
* **ID de Paquete Android:** `org.idmji.biblia`
* **Directorio Base del Código:** `/home/rocua/Proyectos/IDMJI/Biblia_Android`
* **Pipeline Backend y Compendio:** `/home/rocua/Proyectos/IDMJI/Compendio_Doctrinal_IDMJI`
* **Objetivo:** Aplicación nativa Android moderna, rápida y 100% Offline-First para la lectura de la Biblia Reina-Valera 1960 (RVR1960), integrada de forma exclusiva e interactiva con el **Compendio Doctrinal IDMJI** (enseñanzas, estudios bíblicos, meditaciones y preguntas de la Hermana María Luisa Piraquive vinculadas versículo por versículo con marcas de tiempo a YouTube).

---

## 2. Stack Tecnológico y Arquitectura
* **Plataforma:** Android Nativo.
* **Lenguaje:** Kotlin 2.3+ (JVM Toolchain 17).
* **Interfaz de Usuario (UI):** Jetpack Compose con Material 3 (`androidx.compose.material3`).
* **Patrón de Arquitectura:** MVVM (Model - View - ViewModel) con programación reactiva mediante `StateFlow` y Corrutinas de Kotlin en `Dispatchers.IO`.
* **Almacenamiento Local:** Base de datos relacional SQLite interna (`biblia_rvr1960.db`, versión 2), preempaquetada en `assets/` y copiada al almacenamiento interno del dispositivo con migración por versión y preservación de marcadores, notas e historial de usuario.
* **Paradigma de Conectividad:** **Offline-First**. Toda la lectura de la Biblia, el diccionario, las referencias cruzadas y las transcripciones completas del compendio funcionan sin conexión a internet. Solo se requiere conexión para reproducir el video en YouTube o para la sincronización automática de nuevos estudios.

---

## 3. Módulos y Funcionalidades Principales

### A. Lector Bíblico (RVR1960)
* Navegación completa por los 66 libros canónicos (Antiguo y Nuevo Testamento) con selector modal de libros y capítulos.
* **Ajustes de lectura personalizables (`ReadingSettingsSheet`):**
  - Selector de familias tipográficas (Sans Serif, Serif, Monospace).
  - Ajuste dinámico de tamaño de letra (slider de 12sp a 28sp).
  - Altura de línea ajustable.
  - Modos de visualización: Versículo por versículo o en Bloque/Párrafo corrido.
  - Soporte de temas Claro y Oscuro adaptativos.

### B. Barra de Acciones de Versículo (`VerseActionsBar.kt`)
Al tocar un versículo, se despliega una barra contextual organizada en dos niveles:
* **Fila 1 (Herramientas de Estudio Personal):**
  - Paleta de 5 colores para subrayado permanente en SQLite (Amarillo, Verde, Azul, Rosa, Naranja).
  - Copiar versículo al portapapeles.
  - Compartir versículo en formato texto a otras apps.
  - Notas de estudio personales (crear, editar y guardar apuntes ligados al versículo).
* **Fila 2 (Herramientas Doctrinales y Exegéticas - Estrictamente separadas):**
  - `🔗 Referencias Cruzadas`: Sistema tradicional TSK (Treasury of Scripture Knowledge) y notas originales RVR1960 con navegación directa al versículo citado.
  - `📖 Diccionario Bíblico`: Definiciones etimológicas en hebreo y griego (concordancia Strong).
  - `🕊️ Compendio IDMJI`: Abre directamente las enseñanzas específicas de la Hna. María Luisa Piraquive correspondientes a ese versículo exacto.

### C. Compendio Doctrinal IDMJI Versículo a Versículo (`CompendioDialog.kt`)
* **Distintivo Visual en la Lectura:** Los versículos que cuentan con doctrina asociada muestran un icono de palomita blanca `🕊️` interactivo.
* **Catálogo Doctrinal General:** Accesible en cualquier momento desde la barra superior de la app, dividido en dos pestañas:
  1. *Por Pasajes Bíblicos (5,141 intervenciones canónicas):* Desglose por libros canónicos y versículos exactos.
  2. *Por Temas y Preguntas (3,115 intervenciones temáticas):* Preguntas y respuestas, meditaciones y transmisiones sin cita explícita.
  3. *Buscador Doctrinal Global:* Búsqueda en tiempo real por título, cita o palabra clave con límite de 100 resultados, debounce de 300 ms y cancelación de búsquedas anteriores.
* **Visor Detallado de Estudio (`CompendioDetailDialog`):**
  - Transcripción literal y completa del segmento de la enseñanza.
  - **Marcas de tiempo interactivas (`▶ mm:ss` y `▶ hh:mm:ss`):** Chips clicables que abren la aplicación de YouTube en el segundo exacto de la respuesta.
  - Controles de zoom tipográfico (`A-` / `A+`) para facilitar la lectura sin cansar la vista.
  - Botón "Saltar al Pasaje Bíblico" para posicionar el lector en el texto bíblico analizado.

---

## 4. Pipeline Backend y Modelo de Datos

### Estructura de la Tabla SQLite `compendio_idmji`:
```sql
CREATE TABLE compendio_idmji (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tipo TEXT NOT NULL,                -- 'CANONICO' o 'TEMATICO'
    libro_id INTEGER,                  -- ID del libro bíblico (1 a 66) o NULL si es temático
    capitulo INTEGER,                  -- Capítulo bíblico o NULL
    versiculo_inicio INTEGER,          -- Versículo inicial o NULL
    versiculo_fin INTEGER,             -- Versículo final o NULL
    cita TEXT,                         -- Ejemplo: 'Génesis 1:26', 'Mateo 9:22'
    titulo TEXT NOT NULL,              -- Título del estudio o pregunta
    video_url TEXT NOT NULL,           -- URL de YouTube con timestamp base
    video_id TEXT NOT NULL,            -- Identificador de YouTube (ej. X3IFaTH0huk)
    seccion TEXT NOT NULL,             -- Lista de origen (estudios_biblicos, meditaciones, preguntas_y_respuestas, transmisiones_en_vivo)
    transcripcion TEXT NOT NULL        -- Transcripción completa con tags [[mm:ss]](url)
);
CREATE INDEX idx_compendio_verse ON compendio_idmji(libro_id, capitulo, versiculo_inicio, versiculo_fin);
CREATE INDEX idx_compendio_tipo ON compendio_idmji(tipo);
CREATE INDEX idx_compendio_libro ON compendio_idmji(libro_id);
```

### Scripts de Extracción y Actualización en Python:
* `actualizar_compendio_diario.py`: Monitorea las 4 listas oficiales de YouTube de la IDMJI, descarga subtítulos palabra por palabra (con estrategia híbrida Invidious + Tor SOCKS5 fallback ante límites de cuota) y detecta videos nuevos.
* `actualizar_db_android.py`: Segmenta, identifica citas bíblicas mediante expresiones regulares y actualiza la base de datos `biblia_rvr1960.db` empaquetada en la app.

---

## 5. Optimizaciones Críticas Implementadas

1. **Prevención de SQLite CursorWindow Overflow y ANR:**
   - La base de datos contiene más de 80 MB de texto en transcripciones.
   - Las consultas de listados y pestañas solo recuperan metadatos (`'' as transcripcion`); la transcripción completa se carga bajo demanda mediante `getCompendioItemDetail(id)` cuando el usuario abre el visor. Los párrafos se renderizan con `LazyColumn`.
   - Consultas temáticas paginadas (`LIMIT 200 OFFSET ?`) con controles Anterior/Siguiente y ejecución de los accesos SQLite en `Dispatchers.IO`. La creación del helper y la copia inicial se difieren hasta entrar en IO.
   - Se configuró `android:largeHeap="true"` en `AndroidManifest.xml` como margen adicional de memoria; no sustituye la paginación ni garantiza evitar OOM.

2. **Independencia de Fuentes:**
   - Las Referencias Cruzadas tradicionales y el Diccionario Bíblico son 100% independientes del Compendio IDMJI para preservar el rigor exegético y la claridad de la interfaz.

3. **Soporte de Tiempos Extendidos:**
   - Expresión regular adaptativa para soportar tanto formato `MM:SS` como `HH:MM:SS` (ej. `01:07:36`).

---

## 6. Siguiente Paso / Trabajo en Curso: Sincronización Automática Remota
* **Reto:** Se publican nuevos videos y estudios bíblicos semanalmente. No se debe obligar al usuario a reinstalar la app ni a esperar aprobaciones de la Google Play Store para recibir las nuevas enseñanzas.
* **Solución en Desarrollo:**
  - **Backend (Nube):** Un cron o GitHub Action gratuito genera periódicamente un archivo liviano `version_compendio.json` y un delta de novedades (`compendio_novedades.json`, ~30 KB) alojado en GitHub Pages o CDN estática.
  - **Cliente (Android):** Implementación de un WorkManager (`CompendioSyncWorker`) en segundo plano que consulta periódicamente (con Wi-Fi) el delta y hace `INSERT OR IGNORE` en la tabla local `compendio_idmji`.
  - **Acción Manual:** Botón `🔄 Sincronizar enseñanzas` en la barra de la app para actualización instantánea a voluntad del usuario.


## 7. Migraciones y verificación tras la auditoría

* `DB_VERSION` y `PRAGMA user_version` del asset están en **2**. Una nueva versión del contenido empaquetado debe incrementar ambos valores; modificar solo el asset conservando la versión no reemplaza bases ya instaladas.
* `AssetDatabaseInstaller` prepara una copia `.installing`, verifica versión e integridad y restaura todas las columnas de `marcadores` e `historial`, incluidos IDs, valores nulos y fechas. Verifica los datos mediante `EXCEPT` en ambos sentidos antes de instalarla.
* El archivo anterior se conserva como `.previous` durante el reemplazo. Si el proceso se interrumpe entre renombres, se recupera al siguiente inicio. Los errores abortan la actualización; no se omiten silenciosamente ni se permiten downgrades.
* Las cargas del compendio y sus detalles informan errores y tienen cancelación. La búsqueda aplica debounce y las consultas canceladas no publican resultados atrasados.
* Pruebas JVM: `./gradlew testDebugUnitTest`. Pruebas de instalación/migración en Android: `./gradlew connectedDebugAndroidTest`. Compilación: `./gradlew assembleDebug`.
* La verificación y sus limitaciones se documentan en `auditoria/CORRECCIONES.txt`. Se requiere ejecutar las pruebas de migración en un dispositivo/emulador antes de producción.
