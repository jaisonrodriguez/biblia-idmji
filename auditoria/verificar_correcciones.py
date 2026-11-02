from pathlib import Path
import sqlite3, re, json
root=Path(__file__).resolve().parents[1]
helper=(root/'app/src/main/kotlin/org/idmji/biblia/data/BibleDatabaseHelper.kt').read_text()
db=sqlite3.connect((root/'app/src/main/assets/biblia_rvr1960.db').as_uri()+'?mode=ro',uri=True)
queries={}
for name in ['getCompendioForVerse','getCompendioForBook','getThematicCompendio','searchCompendioGlobal','getCompendioItemDetail']:
    body=helper.split('suspend fun '+name+'(',1)[1].split('\n    suspend fun ',1)[0]
    queries[name]=re.findall(r'"""(.*?)"""',body,re.S)
    assert 'withContext(Dispatchers.IO)' in body
    for sql in queries[name]:
        if name!='getCompendioItemDetail': assert "'' as transcripcion" in sql
pages=[];ids=[]
for offset in range(0,3200,200):
    rows=db.execute(queries['getThematicCompendio'][0],[str(offset)]).fetchall()
    assert len(rows)<=200 and all(row[-1]=='' for row in rows)
    pages.append(len(rows));ids.extend(row[0] for row in rows)
assert len(ids)==len(set(ids))==3115
filtered=db.execute(queries['getThematicCompendio'][1],['%fe%','%fe%','0']).fetchall()
assert len(filtered)<=200 and all(r[-1]=='' for r in filtered)
for b,c,v in [(40,9,22),(41,10,52)]:
    rows=db.execute(queries['getCompendioForVerse'][0],[str(b),str(c),str(v)]).fetchall()
    assert rows and all(r[-1]=='' for r in rows)
for id in [3876,4230]:
    row=db.execute(queries['getCompendioItemDetail'][0],[str(id)]).fetchone()
    assert row and row[-1].startswith('[[01:06:57]]')
assert db.execute('PRAGMA integrity_check').fetchone()[0]=='ok'
assert db.execute('PRAGMA user_version').fetchone()[0]==2
print(json.dumps(dict(thematic_pages=pages,total_unique=len(ids),filtered_first_page=len(filtered),detail_cases=[3876,4230],integrity='ok',asset_version=2),indent=2))
