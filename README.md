# SAAR PRF — Sistema de Análise de Acidentes Rodoviários

Aplicação web para importar, consultar, alterar, excluir, filtrar e visualizar os acidentes em rodovias federais publicados pela Polícia Rodoviária Federal.

Projeto 03 · Programação Avançada Orientada a Objetos · FATEC Ipiranga · Grupo 03

## Fonte dos dados

- Portal de Dados Abertos da PRF: https://www.gov.br/prf/pt-br/acesso-a-informacao/dados-abertos/dados-abertos-da-prf
- Conjuntos: "Acidentes — Agrupados por ocorrência" 2025 e 2026 (`datatran2025.zip`, `datatran2026.zip`)
- Dicionário: https://www.gov.br/prf/pt-br/acesso-a-informacao/dados-abertos/dicionario-acidentes
- Licença: CC BY-ND 3.0 · Acesso em 25/10/2026

Os arquivos oficiais não são versionados. Baixe os ZIPs, extraia os CSVs em `dados/brutos/` e gere amostra e carga com:

```bash
python scripts/gerar_amostra.py
```

| Arquivo gerado | Conteúdo |
|---|---|
| `dados/amostra/amostra_demo_prf.csv` | 1.014 linhas reais (amostra estratificada por ano e UF + linhas com problemas reais), importada ao vivo na apresentação |
| `dados/carga/datatran2025_carga.csv` / `datatran2026_carga.csv` | arquivos oficiais menos as linhas da amostra (carga prévia) |
| `dados/testes/invalido_*.csv` | arquivos com erro estrutural, para demonstrar a importação inválida |

## Requisitos

- Java 21 (o Maven vem embutido via `mvnw`)
- Banco PostgreSQL no Neon
- Node.js apenas para editar o TypeScript (o JS compilado já está em `src/main/resources/static/js`)

## Configuração

1. Copie `.env.example` para `.env`.
2. Preencha com os dados do Neon (Dashboard › Connect › Java):
   - `DB_URL` — `jdbc:postgresql://<host>/neondb?sslmode=require&reWriteBatchedInserts=true` (use o host **direto**, sem `-pooler`: o Spring já tem pool próprio e o Hibernate cria o schema)
   - `DB_USERNAME` e `DB_PASSWORD`

O `.env` está no `.gitignore` e nunca deve ser enviado ao GitHub. O `application.properties` só lê as variáveis.

## Execução

```bash
./mvnw spring-boot:run
```

Acesse http://localhost:8080 (página inicial) ou http://localhost:8080/painel.html (painel analítico). A tabela `acidente` é criada automaticamente na primeira execução.

- Testes: `./mvnw test`
- Front-end: `npm install` e `npm run build` (ou `npm run watch`)

## Arquitetura

| Camada | Pacote / pasta | Responsabilidade |
|---|---|---|
| Model | `model` | entidade `Acidente` (JPA + Bean Validation) |
| Repository | `repository` | `AcidenteRepository` (JpaRepository, filtro paginado e agregações para os gráficos) |
| Service | `service` | `AcidenteService` (CRUD, filtro, estatísticas), `ImportacaoService` e `ConversorLinhaCsv` (importação e tratamento) |
| Controller | `controller` | `AcidenteController` (endpoints REST com `ResponseEntity`) |
| Erros | `exception` | `TratadorErros` converte erros de validação e importação em HTTP 400 legível |
| View | `static/` | `index.html` (página inicial), `painel.html` (painel com CRUD, filtros, importação e gráficos), `css/style.css`, `js/` (compilado de `src/main/frontend/ts`), `fonts/` e `img/` |

## Endpoints

| Método | Endpoint | Finalidade | Status |
|---|---|---|---|
| GET | `/api/acidentes?page=0&size=50` | Listar (paginado) | 200 |
| GET | `/api/acidentes/{id}` | Buscar por ID | 200 / 404 |
| POST | `/api/acidentes` | Cadastrar | 201 / 400 |
| PUT | `/api/acidentes/{id}` | Atualizar | 200 / 400 / 404 |
| DELETE | `/api/acidentes/{id}` | Excluir | 204 / 404 |
| POST | `/api/acidentes/importar` | Importar CSV (multipart, campo `arquivo`) | 200 / 400 |
| GET | `/api/acidentes/filtro?uf=&classificacao=&dataInicio=&dataFim=` | Filtrar (critérios combináveis) | 200 |
| GET | `/api/acidentes/estatisticas/causas?(filtros)` | Top 10 causas | 200 |
| GET | `/api/acidentes/estatisticas/mensal?(filtros)` | Total por ano e mês | 200 |
| GET | `/api/acidentes/estatisticas/resumo?(filtros)` | Indicadores dos cards: total, óbitos, feridos, período e rodovia mais crítica | 200 |

Exemplo de cadastro (`POST /api/acidentes`):

```json
{
  "dataOcorrencia": "2025-01-01", "horario": "06:20", "uf": "SP", "municipio": "GUARULHOS",
  "rodovia": "BR-116", "quilometro": 225.0, "causaAcidente": "Reação tardia ou ineficiente do condutor",
  "tipoAcidente": "Tombamento", "classificacao": "Com Vítimas Feridas", "mortos": 0, "feridos": 1,
  "latitude": -23.48586772, "longitude": -46.54075317
}
```

Erro de validação (400):

```json
{ "mensagem": "Dados inválidos", "erros": { "uf": "UF inválida" } }
```

Resultado de importação (200):

```json
{
  "arquivo": "amostra_demo_prf.csv", "linhasLidas": 1014, "importadas": 1006, "rejeitadas": 8, "duplicadas": 0,
  "motivosRejeicao": { "classificação ausente": 2, "rodovia não identificada": 5, "id PRF inválido": 1 },
  "exemplosRejeicao": [ { "linha": 2, "motivo": "classificação ausente" } ],
  "tempoMs": 876, "mensagem": "Importação concluída: 1006 registros importados."
}
```

## Regras de importação

- Erro estrutural bloqueia a importação inteira (HTTP 400): arquivo vazio, extensão diferente de `.csv`, separador diferente de `;`, cabeçalho ausente ou incompleto, codificação UTF-8 com BOM, CSV malformado.
- Linha inválida é rejeitada e contabilizada com o motivo, sem interromper as demais: id PRF inválido (ex.: `8e+05`), classificação ausente (`NA`), rodovia não identificada (`br = 0`), formatos ou valores fora do domínio.
- Normalização: espaços extras, maiúsculas em UF e município, vírgula decimal, data em `aaaa-mm-dd` ou `dd/mm/aaaa`.
- Reimportação não duplica: o `id` original da PRF é gravado em `idPrf` (único); registros repetidos são contados como "duplicados".

Medições com o Neon (região sa-east-1): carga de 2025 (71.924 linhas) em 13,2 s; carga de 2026 (48.303 linhas) em 8,5 s; amostra (1.014 linhas) em 0,9 s.

## Integrantes

- (preencher)
