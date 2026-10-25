"""
Gera a amostra programática de demonstração a partir dos arquivos oficiais da PRF.

A amostra (estratificada por ano e UF, semente fixa, + linhas reais com problemas
conhecidos) é retirada da carga prévia, para que a importação ao vivo insira
registros inéditos em vez de duplicados. As linhas são copiadas byte a byte do
arquivo original, sem reescrever valores.

Uso: python scripts/gerar_amostra.py
"""

import csv
import random
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
BRUTOS = RAIZ / "dados" / "brutos"
ARQUIVOS = ["datatran2025.csv", "datatran2026.csv"]
DIR_AMOSTRA = RAIZ / "dados" / "amostra"
DIR_CARGA = RAIZ / "dados" / "carga"
DIR_TESTES = RAIZ / "dados" / "testes"

ENCODING = "latin-1"
TAMANHO_SORTEIO = 1000      # linhas válidas sorteadas
MIN_POR_ESTRATO = 2         # garante todas as UFs dos dois anos
SEMENTE = 42
QTD_BR_ZERO = 5             # quantas linhas com br=0 entram na amostra
QTD_ESPACO_DUPLO = 2        # quantas linhas com espaço duplo no município entram

UFS = set("AC AL AP AM BA CE DF ES GO MA MT MS MG PA PB PR PE PI RJ RN RS RO RR SC SP SE TO".split())


def ler_arquivo(nome):
    caminho = BRUTOS / nome
    with open(caminho, "rb") as f:
        linhas = f.read().split(b"\r\n")
    if linhas and linhas[-1] == b"":
        linhas.pop()
    cabecalho_bruto = linhas[0]
    colunas = next(csv.reader([cabecalho_bruto.decode(ENCODING)], delimiter=";"))
    registros = []
    for numero, bruta in enumerate(linhas[1:], start=2):
        valores = next(csv.reader([bruta.decode(ENCODING)], delimiter=";"))
        registros.append({
            "arquivo": nome,
            "linha": numero,
            "bruta": bruta,
            "campos": dict(zip(colunas, valores)),
        })
    return cabecalho_bruto, colunas, registros


def problema(campos):
    if not campos["id"].isdigit():
        return "id_invalido"
    if campos["classificacao_acidente"] in ("NA", ""):
        return "classificacao_ausente"
    if campos["br"] == "0":
        return "br_zero"
    if campos["uf"] not in UFS:
        return "uf_invalida"
    if "  " in campos["municipio"]:
        return "espaco_duplo"   # não é rejeição: o importador normaliza
    return None


def main():
    random.seed(SEMENTE)
    cabecalhos, todos = {}, []
    colunas = None
    for nome in ARQUIVOS:
        cab, colunas, regs = ler_arquivo(nome)
        cabecalhos[nome] = cab
        todos.extend(regs)

    # 1) separa linhas válidas por estrato (ano, UF) e linhas problemáticas por tipo
    estratos, problemas = {}, {}
    for r in todos:
        tipo = problema(r["campos"])
        r["problema"] = tipo
        if tipo is None:
            chave = (r["campos"]["data_inversa"][:4], r["campos"]["uf"])
            estratos.setdefault(chave, []).append(r)
        else:
            problemas.setdefault(tipo, []).append(r)

    # 2) alocação proporcional com mínimo por estrato
    total_validas = sum(len(v) for v in estratos.values())
    alocacao = {k: max(MIN_POR_ESTRATO, round(TAMANHO_SORTEIO * len(v) / total_validas))
                for k, v in estratos.items()}
    sorteadas = []
    for chave in sorted(estratos):
        pool = estratos[chave]
        sorteadas.extend(random.sample(pool, min(alocacao[chave], len(pool))))

    # 3) inclusão forçada das linhas problemáticas reais
    forcadas = []
    forcadas += problemas.get("id_invalido", [])
    forcadas += problemas.get("classificacao_ausente", [])
    forcadas += random.sample(problemas.get("br_zero", []), min(QTD_BR_ZERO, len(problemas.get("br_zero", []))))
    forcadas += random.sample(problemas.get("espaco_duplo", []), min(QTD_ESPACO_DUPLO, len(problemas.get("espaco_duplo", []))))

    amostra = sorteadas + forcadas
    amostra.sort(key=lambda r: (r["campos"]["data_inversa"], r["campos"]["horario"]))
    na_amostra = {(r["arquivo"], r["linha"]) for r in amostra}

    # 4) grava amostra, cargas e arquivos inválidos
    for d in (DIR_AMOSTRA, DIR_CARGA, DIR_TESTES):
        d.mkdir(parents=True, exist_ok=True)

    cab = cabecalhos[ARQUIVOS[0]]
    with open(DIR_AMOSTRA / "amostra_demo_prf.csv", "wb") as f:
        f.write(cab + b"\r\n" + b"\r\n".join(r["bruta"] for r in amostra) + b"\r\n")

    contagem_carga = {}
    for nome in ARQUIVOS:
        restantes = [r for r in todos if r["arquivo"] == nome and (nome, r["linha"]) not in na_amostra]
        contagem_carga[nome] = len(restantes)
        saida = DIR_CARGA / nome.replace(".csv", "_carga.csv")
        with open(saida, "wb") as f:
            f.write(cabecalhos[nome] + b"\r\n" + b"\r\n".join(r["bruta"] for r in restantes) + b"\r\n")

    # arquivos com erro estrutural, montados a partir de linhas reais
    exemplo = sorteadas[:5]
    with open(DIR_TESTES / "invalido_separador_virgula.csv", "w", encoding=ENCODING, newline="") as f:
        w = csv.writer(f, delimiter=",", lineterminator="\r\n")
        w.writerow(colunas)
        for r in exemplo:
            w.writerow([r["campos"][c] for c in colunas])
    with open(DIR_TESTES / "invalido_sem_cabecalho.csv", "wb") as f:
        f.write(b"\r\n".join(r["bruta"] for r in exemplo) + b"\r\n")
    (DIR_TESTES / "invalido_vazio.csv").write_bytes(b"")

    # 5) relatório
    tipos_amostra = {}
    for r in forcadas:
        tipos_amostra[r["problema"]] = tipos_amostra.get(r["problema"], 0) + 1
    linhas_rel = [
        "Amostra programática SAAR PRF",
        f"Semente aleatória: {SEMENTE}",
        f"Registros oficiais lidos: {len(todos)} ({', '.join(ARQUIVOS)})",
        f"Linhas válidas (pool de sorteio): {total_validas}",
        f"Estratos (ano, UF): {len(estratos)}  |  mínimo por estrato: {MIN_POR_ESTRATO}",
        f"Linhas sorteadas: {len(sorteadas)}",
        f"Linhas problemáticas incluídas à força: {len(forcadas)} -> {tipos_amostra}",
        f"Total da amostra: {len(amostra)}",
        f"Problemas encontrados na base inteira: { {k: len(v) for k, v in problemas.items()} }",
        "Carga prévia (oficial menos amostra): "
        + ", ".join(f"{k}: {v}" for k, v in contagem_carga.items()),
        f"Conferência: amostra + carga = {len(amostra) + sum(contagem_carga.values())} (deve ser {len(todos)})",
    ]
    texto = "\n".join(linhas_rel)
    (DIR_AMOSTRA / "relatorio_amostra.txt").write_text(texto + "\n", encoding="utf-8")
    print(texto)


if __name__ == "__main__":
    main()
