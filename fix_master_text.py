import glob, os
for p in glob.glob('src/main/resources/templates/*.html'):
    with open(p, 'r', encoding='utf-8') as f:
        s = f.read()
    s = s.replace('<span>MASTER</span>', '<span>DG MASTER</span>')
    with open(p, 'w', encoding='utf-8') as f:
        f.write(s)
    print('updated', os.path.basename(p))
