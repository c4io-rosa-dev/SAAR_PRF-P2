"""Gera Relatorio_Tecnico_SAAR_PRF_Grupo02.pdf a partir de relatorio.html usando o Edge (ou Chrome) em modo headless.

Uso:  python docs/relatorio/gerar_pdf.py

O hash do commit final é lido do Git (HEAD): faça o último commit antes de gerar o PDF.
Imagens ausentes em img/ viram um quadro "pendente" no PDF; figuras marcadas como opcionais são omitidas.
"""
import html
import os
import re
import subprocess
import sys
from pathlib import Path

PASTA = Path(__file__).resolve().parent
RAIZ = PASTA.parent.parent
SAIDA = PASTA / "Relatorio_Tecnico_SAAR_PRF_Grupo02.pdf"
NAVEGADORES = [
    r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
    r"C:\Program Files\Microsoft\Edge\Application\msedge.exe",
    r"C:\Program Files\Google\Chrome\Application\chrome.exe",
]


def git(*args):
    return subprocess.run(["git", *args], cwd=RAIZ, capture_output=True, text=True, check=True).stdout.strip()


def trocar_figuras(texto, pendentes):
    def trocar(m):
        figura, opcional, caminho = m.group(0), m.group(1), m.group(2)
        if (PASTA / caminho).exists():
            return figura
        if opcional:
            return ""
        pendentes.append(caminho)
        quadro = f'<div class="pendente">Inserir captura: docs/relatorio/{caminho}</div>'
        return re.sub(r'<img src="[^"]+">', quadro, figura, count=1)
    return re.sub(r'<figure( class="opcional")?>\s*<img src="(img/[^"]+)">.*?</figure>', trocar, texto, flags=re.S)


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    commit = git("rev-parse", "HEAD")
    data_commit = git("log", "-1", "--format=%cd", "--date=format:%d/%m/%Y %H:%M")
    if git("status", "--porcelain"):
        print("AVISO: há alterações não commitadas; o PDF citará o commit", commit[:7], "que não as inclui.")

    neon = PASTA / "neon.txt"
    texto = (PASTA / "relatorio.html").read_text(encoding="utf-8")
    texto = (texto.replace("{{COMMIT}}", commit)
             .replace("{{COMMIT_CURTO}}", commit[:7])
             .replace("{{DATA_COMMIT}}", data_commit)
             .replace("{{NEON}}", html.escape(neon.read_text(encoding="utf-8")) if neon.exists() else ""))
    pendentes = []
    texto = trocar_figuras(texto, pendentes)

    temporario = PASTA / "_render.html"
    temporario.write_text(texto, encoding="utf-8")
    navegador = next((n for n in NAVEGADORES if os.path.exists(n)), None)
    if not navegador:
        sys.exit("Edge ou Chrome não encontrado.")
    try:
        subprocess.run([navegador, "--headless=new", "--disable-gpu", "--no-pdf-header-footer",
                        "--virtual-time-budget=15000", f"--print-to-pdf={SAIDA}", temporario.as_uri()],
                       check=True, capture_output=True, timeout=180)
    finally:
        temporario.unlink(missing_ok=True)

    print(f"PDF gerado: {SAIDA}")
    print(f"Commit citado: {commit[:7]} ({data_commit})")
    for caminho in pendentes:
        print("Pendente:", caminho)


if __name__ == "__main__":
    main()
