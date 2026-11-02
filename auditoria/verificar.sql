.headers on
.mode column
PRAGMA integrity_check;
PRAGMA user_version;
SELECT tipo,COUNT(*) AS registros FROM compendio_idmji GROUP BY tipo;
SELECT COUNT(*) AS total FROM compendio_idmji;
SELECT COUNT(*) AS canonicos_invalidos FROM compendio_idmji c LEFT JOIN libros l ON l.id=c.libro_id WHERE c.tipo='CANONICO' AND (c.libro_id IS NULL OR c.libro_id NOT BETWEEN 1 AND 66 OR l.id IS NULL);
SELECT COUNT(*) AS libros,MIN(id),MAX(id) FROM libros;
PRAGMA foreign_key_check;
PRAGMA foreign_key_list(compendio_idmji);
SELECT id,cita,libro_id,capitulo,versiculo_inicio,versiculo_fin,substr(transcripcion,1,160) AS muestra FROM compendio_idmji WHERE (libro_id=40 AND capitulo=9 AND versiculo_inicio=22 AND versiculo_fin=22) OR (libro_id=41 AND capitulo=10 AND versiculo_inicio=52 AND versiculo_fin=52);
SELECT COUNT(*) AS registros,SUM(length(CAST(transcripcion AS BLOB))) AS bytes_transcripciones,MAX(length(CAST(transcripcion AS BLOB))) AS max_bytes_transcripcion FROM compendio_idmji;
SELECT name,sql FROM sqlite_master WHERE tbl_name='compendio_idmji';
SELECT libro_id,COUNT(*) AS registros FROM compendio_idmji WHERE tipo='CANONICO' GROUP BY libro_id ORDER BY registros DESC LIMIT 5;
EXPLAIN QUERY PLAN SELECT c.id FROM compendio_idmji c LEFT JOIN libros l ON c.libro_id=l.id WHERE c.titulo LIKE '%fe%' OR c.cita LIKE '%fe%' OR c.seccion LIKE '%fe%' ORDER BY c.tipo ASC,c.id ASC LIMIT 100;
