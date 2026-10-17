import glob, os
for path in glob.glob('src/main/resources/templates/*.html'):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    content = content.replace('th:src="@{/img/logo_dgMaster.png}"', 'src="/img/logo_dgMaster.png"')
    content = content.replace('th:src="@{/img/principal.webp}"', 'src="/img/principal.webp"')
    content = content.replace('th:src="@{/img/img1.jpeg}"', 'src="/img/img1.jpeg"')
    content = content.replace('th:src="@{/img/img2.jpeg}"', 'src="/img/img2.jpeg"')
    content = content.replace('th:src="@{/img/img3.jpeg}"', 'src="/img/img3.jpeg"')
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print('fixed src in', os.path.basename(path))
