import re
import os
import markdown

md_path = r"c:\Users\yunus\Desktop\staj\content_angagement_app\docs\IEEE_RAPORU_TRT_TABII.md"
html_path = r"c:\Users\yunus\Desktop\staj\content_angagement_app\docs\RAPOR_TRT_TABII.html"
pdf_path = r"c:\Users\yunus\Desktop\staj\content_angagement_app\docs\RAPOR_TRT_TABII.pdf"

with open(md_path, "r", encoding="utf-8") as f:
    content = f.read()

# Clean horizontal lines
content_cleaned = re.sub(r'\n\s*---\s*\n', '\n\n', content)

# Header: Only Title, English Title, and Author Block
# Body: Özetçe, Anahtar Kelimeler, Abstract, Keywords, and all Sections (I. GİRİŞ...)
parts = re.split(r'(?=Özetçe\s*—)', content_cleaned, maxsplit=1)
header_md = parts[0] if len(parts) > 1 else ""
body_md = parts[1] if len(parts) > 1 else content_cleaned

# Replace ```mermaid ... ``` with <div class="mermaid-container"><div class="mermaid">\n...\n</div></div>
def replace_mermaid(match):
    code = match.group(1).strip()
    return f'\n\n<div class="mermaid-container"><div class="mermaid">\n{code}\n</div></div>\n\n'

body_md_mermaid = re.sub(r'```mermaid\s+(.*?)\s+```', replace_mermaid, body_md, flags=re.DOTALL)

# Convert Markdown to HTML
header_html = markdown.markdown(header_md, extensions=['extra'])
body_html = markdown.markdown(body_md_mermaid, extensions=['extra', 'tables', 'fenced_code'])

# Format Özetçe, Abstract, Anahtar Kelimeler, Keywords as IEEE bold-italic run-in headings
body_html = re.sub(r'<p>Özetçe\s*—', r'<p class="abstract-p"><strong><em>Özetçe</em>—</strong>', body_html)
body_html = re.sub(r'<p>Anahtar Kelimeler\s*—', r'<p class="keywords-p"><strong><em>Anahtar Kelimeler</em>—</strong>', body_html)
body_html = re.sub(r'<p>Abstract\s*—', r'<p class="abstract-p"><strong><em>Abstract</em>—</strong>', body_html)
body_html = re.sub(r'<p>Keywords\s*—', r'<p class="keywords-p"><strong><em>Keywords</em>—</strong>', body_html)

html_full = f"""<!DOCTYPE html>
<html lang="tr">
<head>
    <meta charset="UTF-8">
    <title>TRT tabii İçerikleri İçin Etkileşim Platformu</title>
    <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
    <style>
        @page {{
            size: A4;
            margin: 1.8cm 1.5cm 1.8cm 1.5cm;
        }}
        
        * {{
            box-sizing: border-box;
        }}
        
        body {{
            font-family: "Times New Roman", Times, "Nimbus Roman No9 L", serif;
            font-size: 10pt;
            line-height: 1.25;
            color: #000000;
            background-color: #ffffff;
            margin: 0 auto;
            max-width: 210mm;
            padding: 5px 15px;
            text-rendering: optimizeLegibility;
        }}
        
        /* BAŞLIK VE YAZAR ALANI (SAYFA BAŞINDA TAM GENİŞLİK) */
        .ieee-header {{
            width: 100%;
            margin-bottom: 16pt;
            text-align: center;
        }}
        
        .ieee-header h1 {{
            font-size: 20pt;
            font-weight: bold;
            line-height: 1.2;
            margin: 0 0 6pt 0;
            border: none;
            padding: 0;
            text-transform: none;
        }}
        
        .ieee-header p:first-of-type {{
            font-size: 12pt;
            font-style: italic;
            margin: 0 0 12pt 0;
            text-indent: 0;
        }}
        
        /* Yazar Bilgisi */
        .ieee-header p:nth-of-type(2) {{
            font-size: 10pt;
            margin: 0 0 8pt 0;
            line-height: 1.35;
            text-indent: 0;
        }}
        
        /* BİLDİRİ GÖVDESİ (ÖZETÇE DAHİL KESİNTİSİZ İKİ SÜTUN) */
        .ieee-body {{
            column-count: 2;
            column-gap: 0.65cm;
            text-align: justify;
            text-justify: inter-word;
        }}
        
        /* Özetçe ve Abstract (İki Sütun İçinde 9pt İtalik) */
        .abstract-p, .keywords-p {{
            font-size: 9pt;
            line-height: 1.2;
            margin: 0 0 6pt 0;
            text-indent: 14pt;
        }}
        
        .keywords-p {{
            margin-bottom: 10pt;
        }}
        
        /* BÖLÜM BAŞLIKLARI (I. GİRİŞ, II. SİSTEM GEREKSİNİMLERİ...) */
        .ieee-body h1 {{
            font-size: 10pt;
            font-weight: bold;
            text-transform: uppercase;
            text-align: center;
            margin-top: 12pt;
            margin-bottom: 4pt;
            page-break-after: avoid;
            break-after: avoid;
            border-bottom: none;
        }}
        
        /* ALT BAŞLIKLAR (A. Fonksiyonel Gereksinimler...) */
        .ieee-body h2 {{
            font-size: 10pt;
            font-weight: bold;
            font-style: italic;
            text-align: left;
            margin-top: 8pt;
            margin-bottom: 3pt;
            page-break-after: avoid;
            break-after: avoid;
            border-bottom: none;
        }}
        
        .ieee-body h3 {{
            font-size: 9.5pt;
            font-weight: bold;
            margin-top: 6pt;
            margin-bottom: 2pt;
            page-break-after: avoid;
            break-after: avoid;
        }}
        
        .ieee-body p {{
            margin: 0 0 4pt 0;
            text-indent: 14pt;
            text-align: justify;
        }}
        
        /* GENİŞ ŞEKİLLER VE TABLOLAR (SAYFAYI KAPLAR) */
        .mermaid-container, table, .table-container {{
            column-span: all;
            margin: 8pt auto;
            text-align: center;
            page-break-inside: avoid;
            break-inside: avoid;
        }}
        
        .mermaid {{
            background: #ffffff;
            border: 0.5pt solid #ccc;
            border-radius: 4px;
            padding: 8pt;
            margin: 4pt auto;
            display: flex;
            justify-content: center;
            max-width: 100%;
        }}
        
        /* TABLOLAR */
        table {{
            width: 100%;
            border-collapse: collapse;
            font-size: 8.5pt;
            line-height: 1.2;
            margin: 6pt 0;
        }}
        
        th, td {{
            border-top: 0.5pt solid #000;
            border-bottom: 0.5pt solid #000;
            padding: 3.5pt 5pt;
            text-align: left;
        }}
        
        th {{
            font-weight: bold;
            border-bottom: 1pt solid #000;
            background-color: #fafafa;
            text-align: center;
        }}
        
        code {{
            font-family: "Courier New", Courier, monospace;
            font-size: 8.5pt;
            background-color: #f6f6f6;
            padding: 1px 2px;
        }}
        
        pre {{
            column-span: all;
            background-color: #f8f8f8;
            border: 0.5pt solid #ddd;
            padding: 6pt;
            font-family: "Courier New", Courier, monospace;
            font-size: 8pt;
            line-height: 1.15;
            overflow-x: auto;
            page-break-inside: avoid;
        }}
        
        pre code {{
            background-color: transparent;
            padding: 0;
        }}
        
        @media print {{
            body {{
                padding: 0;
                color: #000;
            }}
            .ieee-body {{
                column-count: 2;
                column-gap: 0.65cm;
            }}
            .mermaid-container, table, pre {{
                column-span: all;
                page-break-inside: avoid;
            }}
            h1, h2, h3 {{
                page-break-after: avoid;
            }}
        }}
    </style>
</head>
<body>

<div class="ieee-header">
{header_html}
</div>

<div class="ieee-body">
{body_html}
</div>

<script>
    mermaid.initialize({{
        startOnLoad: true,
        theme: 'neutral',
        flowchart: {{ useMaxWidth: true, htmlLabels: true, curve: 'linear' }},
        sequence: {{ useMaxWidth: true, showSequenceNumbers: true }}
    }});
</script>
</body>
</html>
"""

with open(html_path, "w", encoding="utf-8") as f:
    f.write(html_full)

print(f"IEEE HTML with 2-column Abstract generated at: {html_path}")
