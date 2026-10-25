# SAAR PRF — Especificação de design

Projeto 03 · Programação Avançada OO · FATEC Zona Leste · Grupo 03
Data: 25/10/2026 · Entrega do código/relatório: 31/10/2026 · Apresentações: 07/11 e 14/11/2026

## 1. Objetivo

Aplicação web (Spring Boot + PostgreSQL no Neon + HTML/CSS/TypeScript) que importa, armazena, consulta, altera, exclui, filtra e apresenta graficamente os acidentes em rodovias federais publicados pela PRF.

## 2. Dados

### 2.1 Fonte

- Órgão: Polícia Rodoviária Federal — Portal de Dados Abertos da PRF
  (https://www.gov.br/prf/pt-br/acesso-a-informacao/dados-abertos/dados-abertos-da-prf)
- Conjuntos: "Acidentes — Agrupados por ocorrência" 2025 (`datatran2025.zip`) e 2026 (`datatran2026.zip`, jan–ago)
- Dicionário: "Dicionário de Variáveis — dados desagregados por ocorrência — a partir de 2017" (`dados/brutos/dicionario_ocorrencia_2017.pdf`)
- Licença: CC BY-ND 3.0 · Data de acesso: 25/10/2026

### 2.2 Perfil

| Item | 2025 | 2026 |
|---|---|---|
| Registros | 72.529 | 48.712 |
| Período | 01/01–31/12/2025 | 01/01–31/08/2026 |
| Tamanho CSV | 21,1 MB | 14,2 MB |

Formato: ISO-8859-1, CRLF, separador `;`, texto entre aspas (alguns campos contêm `;` dentro das aspas), decimal com vírgula, ausente = `NA`, 30 colunas, cabeçalhos idênticos nos dois anos.

Divergências entre dicionário e arquivo real: data em `aaaa-mm-dd` (dicionário: `dd/mm/aaaa`); km com vírgula (dicionário: ponto); km 0 presente em rodovias identificadas (dicionário: mínimo 0,1). O importador aceita os dois formatos e km 0.

### 2.3 Problemas reais encontrados

| Problema | Casos | Tratamento |
|---|---|---|
| `id` em notação científica (`8e+05`) | 1 | rejeita: "id PRF inválido" |
| `classificacao_acidente` = `NA` | 2 | rejeita: "classificação ausente" |
| `br = 0` (rodovia não identificada; sempre com `km = 0`) | 267 | rejeita: "rodovia não identificada" |
| `km = 0` com rodovia identificada | 285 | aceita: marco inicial da rodovia (diverge do mínimo de 0,1 km citado no dicionário) |
| espaço duplo em município | 16 | normaliza (trim + colapsa espaços) |
| duplicatas por `id` | 0 na base | `idPrf` UNIQUE; repetidos contam como "duplicados" |
| duplicatas lógicas (mesmo local/minuto) | 57 grupos | mantidas: coordenadas e causas diferentes, são ocorrências distintas |

### 2.4 Amostra e carga

`scripts/gerar_amostra.py` (semente 42) gera:

- `dados/amostra/amostra_demo_prf.csv` — 1.014 linhas: 1.004 válidas sorteadas por estratificação proporcional (ano, UF), mínimo 2 por estrato, + 10 linhas reais com problemas (1 id inválido, 2 NA, 5 br=0, 2 espaço duplo). Importada ao vivo.
- `dados/carga/datatran2025_carga.csv` (71.924) e `datatran2026_carga.csv` (48.303) — oficial menos amostra. Carga prévia antes da apresentação.
- `dados/testes/invalido_{separador_virgula,sem_cabecalho,vazio}.csv` — demonstração de erro estrutural.

Resultado esperado da demo: 1.014 lidas · 1.006 importadas · 8 rejeitadas · 0 duplicadas (na reimportação: 1.006 duplicadas).

Brutos, carga e amostra ficam fora do GitHub (licença CC BY-ND e tamanho); o repositório leva o script e os links oficiais.

## 3. Entidade `Acidente`

| Atributo | Tipo Java | Obrigatório | Origem | Validação / regra |
|---|---|---|---|---|
| id | Long | gerado | — | SEQUENCE (allocationSize 1000, alinhado ao batch de 1000) |
| idPrf | Long | não (nulo em cadastro manual) | `id` | só dígitos; UNIQUE |
| dataOcorrencia | LocalDate | sim | `data_inversa` | `aaaa-mm-dd` ou `dd/mm/aaaa`; não futura |
| horario | LocalTime | sim | `horario` | `HH:mm:ss` ou `HH:mm` |
| uf | String(2) | sim | `uf` | trim + upper; uma das 27 UFs |
| municipio | String(60) | sim | `municipio` | trim + colapsa espaços + upper |
| rodovia | String(6) | sim | `br` | inteiro > 0, gravado como `BR-116`; BR-000 rejeitada |
| quilometro | BigDecimal(7,1) | sim | `km` | vírgula ou ponto; 0 a 9999,9 |
| causaAcidente | String(120) | sim | `causa_acidente` | trim; não vazio |
| tipoAcidente | String(60) | sim | `tipo_acidente` | trim; não vazio |
| classificacao | String(25) | sim | `classificacao_acidente` | Sem Vítimas · Com Vítimas Feridas · Com Vítimas Fatais · Ignorado |
| mortos | Integer | sim | `mortos` | ≥ 0 |
| feridos | Integer | sim | `feridos` | ≥ 0 |
| latitude | BigDecimal(10,7) | sim | `latitude` | vírgula ou ponto; −34 a 6 |
| longitude | BigDecimal(10,7) | sim | `longitude` | vírgula ou ponto; −74 a −34 |

Índices: `uf`, `data_ocorrencia`, `classificacao`.

Colunas descartadas: `dia_semana` (derivável da data); `feridos_leves`, `feridos_graves` (total em `feridos`); `pessoas` (diverge da soma de vítimas em ~5% das linhas); `ilesos`, `ignorados`, `veiculos`, `fase_dia`, `sentido_via`, `condicao_metereologica`, `tipo_pista`, `tracado_via`, `uso_solo` (fora do foco das análises); `regional`, `delegacia`, `uop` (administrativas da PRF).

## 4. Arquitetura

### 4.1 Stack

- Java 21, Spring Boot (versão estável atual do Initializr), Maven Wrapper
- Spring Web, Spring Data JPA, Bean Validation, driver PostgreSQL, Apache Commons CSV
- PostgreSQL no Neon
- Front: HTML + CSS + **TypeScript** compilado para JS, `fetch()`, Chart.js

### 4.2 Pacotes (`br.edu.fatec.saarprf`)

| Pacote | Classes | Responsabilidade |
|---|---|---|
| model | `Acidente` | entidade JPA |
| repository | `AcidenteRepository` | JpaRepository; filtro paginado (JPQL com parâmetros opcionais); agregações por causa e por ano/mês; busca dos `idPrf` existentes |
| service | `AcidenteService` | CRUD, validação de domínio, filtro e estatísticas |
| service | `ImportacaoService` | validação estrutural, leitura em fluxo, deduplicação, gravação em lotes |
| service | `ConversorLinhaCsv` | uma linha CSV → `Acidente` ou motivo de rejeição; sem dependência de banco |
| controller | `AcidenteController` | endpoints com `ResponseEntity` |
| dto | `ResultadoImportacao`, `PaginaResposta<T>`, `ContagemCausa`, `ContagemMensal`, `ErroResposta` | contratos JSON |
| exception | `ImportacaoException`, `TratadorErros` (`@RestControllerAdvice`) | erros → 400 com mensagem legível |

### 4.3 Configuração e segurança

- `application.properties`: `spring.datasource.url=${DB_URL}`, `username=${DB_USERNAME}`, `password=${DB_PASSWORD}`; `spring.config.import=optional:file:.env[.properties]`
- `.env` no `.gitignore`; `.env.example` sem valores; instruções no README
- `spring.jpa.hibernate.ddl-auto=update`
- `hibernate.jdbc.batch_size=1000`, `order_inserts=true`, `reWriteBatchedInserts=true` na URL
- `spring.servlet.multipart.max-file-size=50MB`, `max-request-size=50MB`

## 5. API REST

| Método | Rota | Status | Observação |
|---|---|---|---|
| GET | `/api/acidentes?page=0&size=50` | 200 | paginado, ordenado por data desc; `totalElements` alimenta o contador |
| GET | `/api/acidentes/{id}` | 200 / 404 | `Optional` |
| POST | `/api/acidentes` | 201 / 400 | cadastro manual |
| PUT | `/api/acidentes/{id}` | 200 / 400 / 404 | |
| DELETE | `/api/acidentes/{id}` | 204 / 404 | |
| POST | `/api/acidentes/importar` | 200 / 400 | multipart, campo `arquivo` |
| GET | `/api/acidentes/filtro?uf=&classificacao=&dataInicio=&dataFim=&page=&size=` | 200 | filtros combináveis e opcionais |
| GET | `/api/acidentes/estatisticas/causas?(filtros)` | 200 | top 10 causas |
| GET | `/api/acidentes/estatisticas/mensal?(filtros)` | 200 | total por ano/mês |

Erro de validação (400):

```json
{ "mensagem": "Dados inválidos", "erros": { "uf": "UF inválida: XX" } }
```

Resultado da importação (200):

```json
{
  "arquivo": "amostra_demo_prf.csv",
  "linhasLidas": 1014, "importadas": 1006, "rejeitadas": 8, "duplicadas": 0,
  "motivosRejeicao": { "rodovia não identificada": 5, "classificação ausente": 2, "id PRF inválido": 1 },
  "exemplosRejeicao": [ { "linha": 37, "motivo": "rodovia não identificada" } ],
  "tempoMs": 2300,
  "mensagem": "Importação concluída: 1006 registros importados."
}
```

## 6. Importação

1. Validação estrutural (falha → 400, nada é gravado): arquivo vazio; extensão ≠ `.csv`; cabeçalho ausente ou sem as colunas obrigatórias; separador ≠ `;`.
2. Leitura em fluxo (ISO-8859-1, Commons CSV com `;` e aspas); cada linha passa pelo `ConversorLinhaCsv`.
3. Deduplicação: `HashSet` com os `idPrf` já gravados + os vistos no próprio arquivo.
4. Gravação: `saveAll` em lotes de 1.000 com `flush()`/`clear()`, numa única transação.
5. Resposta `ResultadoImportacao`; primeiras 20 rejeições com linha e motivo.

Políticas: linha inválida é rejeitada e contabilizada sem interromper a importação; reimportar o mesmo arquivo não duplica (registros contados como "duplicados").

## 7. Interface

Página única `index.html`:

- cabeçalho com contador de registros
- card de importação (seleção de arquivo, envio via `FormData`, resultado detalhado)
- barra de filtros (UF, classificação, período) + busca por ID
- gráfico A: top 10 causas, barras horizontais — "Quais são as principais causas de acidentes no recorte filtrado?"
- gráfico C: evolução mensal, linhas 2025 × 2026 — "Como os acidentes evoluem mês a mês e como 2026 se compara a 2025?"
- tabela paginada com ações Editar / Excluir
- formulário de cadastro/edição (UF e classificação em `<select>`, causa com `<datalist>`)
- mensagens de sucesso/erro (toast)

Gráficos e tabela usam os mesmos filtros ativos. Layout visual será definido em etapa própria de design.

### 7.1 TypeScript

- Fontes em `src/main/frontend/ts/` (`api.ts`, `tipos.ts`, `tabela.ts`, `formulario.ts`, `graficos.ts`, `importacao.ts`, `app.ts`)
- `tsc` compila para `src/main/resources/static/js/` (ES modules; `<script type="module">`)
- `package.json` + `tsconfig.json` na raiz; `npm run build` / `npm run watch`
- JS compilado é versionado, para a aplicação rodar só com `mvnw` (sem Node)
- Chart.js carregado por CDN; pacote `chart.js` como devDependency apenas para tipos

## 8. Testes

- Unitários de `ConversorLinhaCsv`: linha válida; `8e+05`; `NA` em classificação; `br=0`; espaço duplo; vírgula decimal; data `dd/mm/aaaa`; UF inválida; lat/lon fora do Brasil
- Unitários de `ImportacaoService` (repositório mockado): erros estruturais e contagem de duplicados
- `@WebMvcTest` de `AcidenteController`: 200, 201, 204, 400, 404
- Medição real do tempo de importação da carga no Neon
- Testes no Insomnia (etapa de evidências)

## 9. Convenções

- Comentários só onde houver regra de negócio ou anotação pontual
- Nomes em português, alinhados ao enunciado

## 10. Fora de escopo agora

Relatório técnico, repositório GitHub, evidências — tratados em etapas separadas.
