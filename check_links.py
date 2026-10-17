import glob
for p in glob.glob('src/main/resources/templates/*.html'):
    s = open(p, 'r', encoding='utf-8').read()
    has_catalogo = 'th:href="@{/catalogo}"' in s
    has_root = 'th:href="@{/}"' in s
    if has_catalogo or has_root:
        print(p, 'catalogo=', has_catalogo, 'root=', has_root)
