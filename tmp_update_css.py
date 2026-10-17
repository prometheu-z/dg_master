import glob, os
for path in glob.glob('src/main/resources/templates/*.html'):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    content = content.replace('th:href="@{/css/app.css}"', 'th:href="@{/css/style.css}"')
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print('updated', os.path.basename(path))
