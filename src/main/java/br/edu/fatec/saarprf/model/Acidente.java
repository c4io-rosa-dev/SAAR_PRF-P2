package br.edu.fatec.saarprf.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "acidente", indexes = {
        @Index(name = "idx_acidente_uf", columnList = "uf"),
        @Index(name = "idx_acidente_data", columnList = "data_ocorrencia"),
        @Index(name = "idx_acidente_classificacao", columnList = "classificacao")
})
public class Acidente {

    public static final String UFS =
            "AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO";

    // domínio definido pelo dicionário oficial da PRF
    public static final String CLASSIFICACOES =
            "Sem Vítimas|Com Vítimas Feridas|Com Vítimas Fatais|Ignorado";

    @Id
    // a sequence reserva 1000 ids por consulta, para a importação em lote não ir ao banco a cada registro
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "acidente_seq")
    @SequenceGenerator(name = "acidente_seq", sequenceName = "acidente_seq", allocationSize = 1000)
    private Long id;

    @Column(unique = true)
    private Long idPrf;

    @NotNull(message = "data da ocorrência é obrigatória")
    @PastOrPresent(message = "data da ocorrência não pode ser futura")
    @Column(nullable = false)
    private LocalDate dataOcorrencia;

    @NotNull(message = "horário é obrigatório")
    @Column(nullable = false)
    private LocalTime horario;

    @NotBlank(message = "UF é obrigatória")
    @Pattern(regexp = UFS, message = "UF inválida")
    @Column(nullable = false, length = 2)
    private String uf;

    @NotBlank(message = "município é obrigatório")
    @Size(max = 60, message = "município deve ter até 60 caracteres")
    @Column(nullable = false, length = 60)
    private String municipio;

    @NotBlank(message = "rodovia é obrigatória")
    @Pattern(regexp = "BR-(?!000)\\d{3}", message = "rodovia inválida: use o formato BR-000 (ex.: BR-116)")
    @Column(nullable = false, length = 6)
    private String rodovia;

    // o dicionário cita 0,1 km como mínimo, mas o arquivo tem km 0 em rodovias identificadas (marco inicial):
    // aceitamos 0; rodovia não identificada (br = 0) é rejeitada no ConversorLinhaCsv
    @NotNull(message = "quilômetro é obrigatório")
    @DecimalMin(value = "0.0", message = "quilômetro não pode ser negativo")
    @DecimalMax(value = "9999.9", message = "quilômetro fora do intervalo válido")
    @Column(nullable = false, precision = 7, scale = 1)
    private BigDecimal quilometro;

    @NotBlank(message = "causa do acidente é obrigatória")
    @Size(max = 120, message = "causa deve ter até 120 caracteres")
    @Column(nullable = false, length = 120)
    private String causaAcidente;

    @NotBlank(message = "tipo do acidente é obrigatório")
    @Size(max = 60, message = "tipo deve ter até 60 caracteres")
    @Column(nullable = false, length = 60)
    private String tipoAcidente;

    @NotBlank(message = "classificação é obrigatória")
    @Pattern(regexp = CLASSIFICACOES, message = "classificação inválida")
    @Column(nullable = false, length = 25)
    private String classificacao;

    @NotNull(message = "número de mortos é obrigatório")
    @Min(value = 0, message = "número de mortos não pode ser negativo")
    @Column(nullable = false)
    private Integer mortos;

    @NotNull(message = "número de feridos é obrigatório")
    @Min(value = 0, message = "número de feridos não pode ser negativo")
    @Column(nullable = false)
    private Integer feridos;

    // limites aproximados do território brasileiro
    @NotNull(message = "latitude é obrigatória")
    @DecimalMin(value = "-34.0", message = "latitude fora do território brasileiro")
    @DecimalMax(value = "6.0", message = "latitude fora do território brasileiro")
    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @NotNull(message = "longitude é obrigatória")
    @DecimalMin(value = "-74.0", message = "longitude fora do território brasileiro")
    @DecimalMax(value = "-34.0", message = "longitude fora do território brasileiro")
    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdPrf() {
        return idPrf;
    }

    public void setIdPrf(Long idPrf) {
        this.idPrf = idPrf;
    }

    public LocalDate getDataOcorrencia() {
        return dataOcorrencia;
    }

    public void setDataOcorrencia(LocalDate dataOcorrencia) {
        this.dataOcorrencia = dataOcorrencia;
    }

    public LocalTime getHorario() {
        return horario;
    }

    public void setHorario(LocalTime horario) {
        this.horario = horario;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public String getMunicipio() {
        return municipio;
    }

    public void setMunicipio(String municipio) {
        this.municipio = municipio;
    }

    public String getRodovia() {
        return rodovia;
    }

    public void setRodovia(String rodovia) {
        this.rodovia = rodovia;
    }

    public BigDecimal getQuilometro() {
        return quilometro;
    }

    public void setQuilometro(BigDecimal quilometro) {
        this.quilometro = quilometro;
    }

    public String getCausaAcidente() {
        return causaAcidente;
    }

    public void setCausaAcidente(String causaAcidente) {
        this.causaAcidente = causaAcidente;
    }

    public String getTipoAcidente() {
        return tipoAcidente;
    }

    public void setTipoAcidente(String tipoAcidente) {
        this.tipoAcidente = tipoAcidente;
    }

    public String getClassificacao() {
        return classificacao;
    }

    public void setClassificacao(String classificacao) {
        this.classificacao = classificacao;
    }

    public Integer getMortos() {
        return mortos;
    }

    public void setMortos(Integer mortos) {
        this.mortos = mortos;
    }

    public Integer getFeridos() {
        return feridos;
    }

    public void setFeridos(Integer feridos) {
        this.feridos = feridos;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }
}
