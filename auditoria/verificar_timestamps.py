import sqlite3, re, json, hashlib
from pathlib import Path
from urllib.parse import urlparse, parse_qs
root=Path(__file__).resolve().parents[1]
asset=root/'app/src/main/assets/biblia_rvr1960.db'
db=sqlite3.connect(f'{asset.as_uri()}?mode=ro', uri=True)
valid=re.compile(r'\[\[(\d{1,2}:\d{2}(?::\d{2})?)\]\]\((https?://[^\)]+)\)')
current=re.compile(r'\[\[(\d{1,2}:\d{2})\]\]\((https?://[^\)]+)\)')
counts=dict(tags=0,mmss=0,hhmmss=0,invalid_clock=0,invalid_host=0,missing_or_wrong_seconds=0,video_id_mismatch=0,records_without_tags=0,records_with_hour_tags=0)
cases=[]
for id,b,c,vs,ve,cita,video_id,text in db.execute('SELECT id,libro_id,capitulo,versiculo_inicio,versiculo_fin,cita,video_id,transcripcion FROM compendio_idmji'):
    tags=valid.findall(text or '')
    if not tags: counts['records_without_tags']+=1
    if any(t.count(':')==2 for t,u in tags): counts['records_with_hour_tags']+=1
    for label,url in tags:
        parts=list(map(int,label.split(':')))
        counts['tags']+=1
        counts['hhmmss' if len(parts)==3 else 'mmss']+=1
        if parts[-1]>=60 or (len(parts)==3 and parts[-2]>=60): counts['invalid_clock']+=1
        seconds=parts[0]*60+parts[1] if len(parts)==2 else parts[0]*3600+parts[1]*60+parts[2]
        parsed=urlparse(url); params=parse_qs(parsed.query)
        if parsed.hostname not in ('www.youtube.com','youtube.com','youtu.be','m.youtube.com'): counts['invalid_host']+=1
        if params.get('t')!=[f'{seconds}s']: counts['missing_or_wrong_seconds']+=1
        if params.get('v')!=[video_id]: counts['video_id_mismatch']+=1
    if (b,c,vs,ve) in ((40,9,22,22),(41,10,52,52)):
        cases.append(dict(id=id,cita=cita,valid_tags=len(tags),matched_by_current_parser=len(current.findall(text or '')),first_tags=tags[:2]))
print(json.dumps(dict(asset=str(asset.relative_to(root)),asset_bytes=asset.stat().st_size,sha256=hashlib.file_digest(asset.open('rb'),'sha256').hexdigest(),counts=counts,cases=cases),indent=2,ensure_ascii=False))
