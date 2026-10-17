import glob, os
for path in glob.glob('src/main/resources/templates/*.html'):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    content = content.replace('<a class="brand" th:href="@{/catalogo}">', '<a class="brand" th:href="@{/}">')
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print('fixed brand in', os.path.basename(path))
