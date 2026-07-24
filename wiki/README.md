# Wiki source

These Markdown files are the source for the project's **GitHub Wiki**. They're kept here in the main
repo so the wiki is versioned and reviewable; the live wiki is a separate `*.wiki.git` repository.

## Publishing to the GitHub Wiki

The wiki repo only exists after the wiki has been initialized once:

1. On GitHub: **Settings → Features → Wikis** (enable), then open the **Wiki** tab and **Create the first page** (any content) and save. This creates `https://github.com/<owner>/<repo>.wiki.git`.
2. Then mirror these files into it:

```bash
git clone https://github.com/<owner>/<repo>.wiki.git wiki-live
cp wiki/*.md wiki-live/
cd wiki-live
git add .
git commit -m "docs: sync wiki from repo"
git push
```

Page names map to file names: `Home.md` → the landing page, `Skript-Syntax.md` → the “Skript Syntax” page, `_Sidebar.md` → the sidebar. Links between pages use the page name without `.md`, e.g. `[Configuration](Configuration)`.
