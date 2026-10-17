import glob, os
for path in glob.glob('src/main/resources/templates/*.html'):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    content = content.replace('<span class="brand-mark">DG</span>', '<span class="brand-mark"><img src="/img/logo_dgMaster.png" alt="DG Master"></span>')
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print('updated logo in', os.path.basename(path))
