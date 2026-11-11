package br.edu.ifsp.carregaja.control;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import br.edu.ifsp.carregaja.service.RegraNegocioException;
import br.edu.ifsp.carregaja.view.ErroResponse;

import lombok.extern.slf4j.Slf4j;

/** Toda recusa sai como {"mensagem": "..."}, para a tela mostrar o motivo. */
@Slf4j
@RestControllerAdvice
public class TratadorDeErros {

  @ExceptionHandler(RegraNegocioException.class)
  public ResponseEntity<ErroResponse> regraNegocio(RegraNegocioException erro) {
    log.warn("Recusado -> {}: {}", erro.getStatus().value(), erro.getMessage());
    return ResponseEntity.status(erro.getStatus()).body(new ErroResponse(erro.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException erro) {
    String mensagem = erro.getBindingResult().getAllErrors().stream()
        .map(ObjectError::getDefaultMessage)
        .findFirst()
        .orElse("Dados inválidos.");
    log.warn("Dados inválidos -> 400: {}", mensagem);
    return ResponseEntity.badRequest().body(new ErroResponse(mensagem));
  }

  // Parâmetro da URL com formato inválido, como uma data fora de aaaa-mm-dd
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErroResponse> parametroInvalido(MethodArgumentTypeMismatchException erro) {
    log.warn("Parâmetro '{}' inválido -> 400", erro.getName());
    return ResponseEntity.badRequest().body(new ErroResponse("O parâmetro '" + erro.getName() + "' é inválido."));
  }

  // Dois pedidos simultâneos passam pela verificação do serviço, mas só um passa pela restrição do banco
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErroResponse> integridade(DataIntegrityViolationException erro) {
    log.warn("Violação de integridade -> 409: {}", erro.getMostSpecificCause().getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErroResponse("Este registro já existe ou está em uso."));
  }
}
