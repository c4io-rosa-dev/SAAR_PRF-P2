package br.edu.fatec.saarprf.exception;

import br.edu.fatec.saarprf.dto.ErroResposta;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class TratadorErros {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> validacao(MethodArgumentNotValidException e) {
        Map<String, String> erros = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(erro -> erros.putIfAbsent(erro.getField(), erro.isBindingFailure()
                        ? "valor em formato inválido (datas no formato aaaa-mm-dd)"
                        : erro.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErroResposta("Dados inválidos", erros));
    }

    @ExceptionHandler(ImportacaoException.class)
    public ResponseEntity<ErroResposta> importacao(ImportacaoException e) {
        return erro(e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErroResposta> arquivoGrande(MaxUploadSizeExceededException e) {
        return erro("Arquivo maior que o limite de 50 MB.");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErroResposta> semArquivo(MissingServletRequestPartException e) {
        return erro("Nenhum arquivo enviado: use o campo 'arquivo'.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> jsonInvalido(HttpMessageNotReadableException e) {
        return erro("JSON inválido ou ausente: verifique os formatos de data (aaaa-mm-dd), horário (hh:mm ou hh:mm:ss) e números.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResposta> parametroInvalido(MethodArgumentTypeMismatchException e) {
        return erro("Parâmetro inválido: " + e.getName());
    }

    private ResponseEntity<ErroResposta> erro(String mensagem) {
        return ResponseEntity.badRequest().body(new ErroResposta(mensagem, Map.of()));
    }
}
