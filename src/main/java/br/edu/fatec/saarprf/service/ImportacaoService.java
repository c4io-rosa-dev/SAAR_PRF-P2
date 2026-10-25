package br.edu.fatec.saarprf.service;

import br.edu.fatec.saarprf.dto.RejeicaoLinha;
import br.edu.fatec.saarprf.dto.ResultadoImportacao;
import br.edu.fatec.saarprf.exception.ImportacaoException;
import br.edu.fatec.saarprf.model.Acidente;
import br.edu.fatec.saarprf.repository.AcidenteRepository;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ImportacaoService {

    static final List<String> COLUNAS_OBRIGATORIAS = List.of(
            "id", "data_inversa", "horario", "uf", "br", "km", "municipio", "causa_acidente",
            "tipo_acidente", "classificacao_acidente", "mortos", "feridos", "latitude", "longitude");

    private static final int TAMANHO_LOTE = 1000;
    private static final int MAXIMO_EXEMPLOS = 20;

    private final AcidenteRepository repository;
    private final ConversorLinhaCsv conversor;
    private final Validator validator;
    private final EntityManager entityManager;

    public ImportacaoService(AcidenteRepository repository, ConversorLinhaCsv conversor,
                             Validator validator, EntityManager entityManager) {
        this.repository = repository;
        this.conversor = conversor;
        this.validator = validator;
        this.entityManager = entityManager;
    }

    @Transactional
    public ResultadoImportacao importar(MultipartFile arquivo) {
        long inicio = System.currentTimeMillis();
        validarArquivo(arquivo);

        // os arquivos oficiais da PRF são publicados em ISO-8859-1, não em UTF-8
        try (BufferedReader leitor = new BufferedReader(
                new InputStreamReader(arquivo.getInputStream(), StandardCharsets.ISO_8859_1))) {

            String[] colunas = lerCabecalho(leitor);
            CSVFormat formato = CSVFormat.Builder.create()
                    .setDelimiter(';')
                    .setQuote('"')
                    .setHeader(colunas)
                    .get();

            // ids PRF já gravados + os vistos neste arquivo: reimportar o mesmo arquivo não duplica registros
            Set<Long> idsVistos = new HashSet<>(repository.buscarIdsPrf());
            Map<String, Integer> motivos = new LinkedHashMap<>();
            List<RejeicaoLinha> exemplos = new ArrayList<>();
            List<Acidente> lote = new ArrayList<>();
            int lidas = 0;
            int importadas = 0;
            int rejeitadas = 0;
            int duplicadas = 0;

            for (CSVRecord registro : formato.parse(leitor)) {
                lidas++;
                long numeroLinha = registro.getRecordNumber() + 1;
                try {
                    Acidente acidente = converterEValidar(registro);
                    if (!idsVistos.add(acidente.getIdPrf())) {
                        duplicadas++;
                        continue;
                    }
                    lote.add(acidente);
                    importadas++;
                    if (lote.size() == TAMANHO_LOTE) {
                        gravar(lote);
                    }
                } catch (LinhaInvalidaException e) {
                    // linha inválida não interrompe a importação: é contabilizada com o motivo
                    rejeitadas++;
                    motivos.merge(e.getMessage(), 1, Integer::sum);
                    if (exemplos.size() < MAXIMO_EXEMPLOS) {
                        exemplos.add(new RejeicaoLinha(numeroLinha, e.getMessage()));
                    }
                }
            }
            gravar(lote);

            if (lidas == 0) {
                throw new ImportacaoException("O arquivo não contém registros após o cabeçalho.");
            }

            String mensagem = importadas > 0
                    ? "Importação concluída: %d registros importados.".formatted(importadas)
                    : "Nenhum registro novo importado (%d duplicados, %d rejeitados).".formatted(duplicadas, rejeitadas);
            return new ResultadoImportacao(arquivo.getOriginalFilename(), lidas, importadas, rejeitadas,
                    duplicadas, motivos, exemplos, System.currentTimeMillis() - inicio, mensagem);

        } catch (UncheckedIOException e) {
            throw new ImportacaoException("Arquivo CSV malformado: verifique aspas e separadores.");
        } catch (IOException e) {
            throw new ImportacaoException("Não foi possível ler o arquivo.");
        }
    }

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ImportacaoException("Arquivo vazio: selecione um arquivo CSV com dados.");
        }
        String nome = arquivo.getOriginalFilename();
        if (nome == null || !nome.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new ImportacaoException("Extensão inválida: envie um arquivo .csv.");
        }
    }

    private String[] lerCabecalho(BufferedReader leitor) throws IOException {
        String linha = leitor.readLine();
        if (linha == null || linha.isBlank()) {
            throw new ImportacaoException("Cabeçalho ausente ou incompleto: a primeira linha está vazia.");
        }
        // BOM UTF-8 lido como ISO-8859-1 vira "ï»¿" no início do cabeçalho
        if (linha.startsWith("ï»¿")) {
            throw new ImportacaoException("Codificação incorreta: envie o arquivo em ISO-8859-1, como publicado pela PRF.");
        }
        if (!linha.contains(";")) {
            throw new ImportacaoException("Separador incorreto: o arquivo deve usar ponto e vírgula (;) entre as colunas.");
        }
        String[] colunas = Arrays.stream(linha.split(";"))
                .map(coluna -> coluna.replace("\"", "").trim())
                .toArray(String[]::new);
        if (Arrays.stream(colunas).anyMatch(String::isBlank)) {
            throw new ImportacaoException("Cabeçalho ausente ou incompleto: há colunas sem nome no cabeçalho.");
        }
        List<String> presentes = Arrays.asList(colunas);
        List<String> faltando = COLUNAS_OBRIGATORIAS.stream().filter(c -> !presentes.contains(c)).toList();
        if (!faltando.isEmpty()) {
            throw new ImportacaoException("Cabeçalho ausente ou incompleto: faltam as colunas "
                    + String.join(", ", faltando) + ".");
        }
        return colunas;
    }

    private Acidente converterEValidar(CSVRecord registro) {
        if (!registro.isConsistent()) {
            throw new LinhaInvalidaException("número de colunas incorreto");
        }
        Acidente acidente = conversor.converter(registro.toMap());
        validator.validate(acidente).stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .findFirst()
                .ifPresent(motivo -> {
                    throw new LinhaInvalidaException(motivo);
                });
        return acidente;
    }

    // flush + clear a cada lote mantém a memória estável mesmo com 70 mil registros
    private void gravar(List<Acidente> lote) {
        if (lote.isEmpty()) {
            return;
        }
        repository.saveAll(List.copyOf(lote));
        entityManager.flush();
        entityManager.clear();
        lote.clear();
    }
}
