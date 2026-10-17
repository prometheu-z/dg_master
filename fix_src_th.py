import glob, os
for path in glob.glob('src/main/resources/templates/*.html'):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    # Substitui src estático por th:src para resolver contexto corretamente
    content = content.replace('src="/img/logo_dgMaster.png"', 'th:src="@{/img/logo_dgMaster.png}"')
    content = content.replace('src="/img/principal.webp"', 'th:src="@{/img/principal.webp}"')
    content = content.replace('src="/img/img1.jpeg"', 'th:src="@{/img/img1.jpeg}"')
    content = content.replace('src="/img/img2.jpeg"', 'th:src="@{/img/img2.jpeg}"')
    content = content.replace('src="/img/img3.jpeg"', 'th:src="@{/img/img3.jpeg}"')
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print('updated', os.path.basename(path))
