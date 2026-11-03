package br.edu.ifsp.carregaja.control;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.edu.ifsp.carregaja.service.RegraNegocioException;
import br.edu.ifsp.carregaja.view.ErroResponse;

/** Devolve os erros no formato {"mensagem": "..."}, que a tela usa para mostrar o motivo. */
@RestControllerAdvice
public class TratadorDeErros {

  @ExceptionHandler(RegraNegocioException.class)
  public ResponseEntity<ErroResponse> regraNegocio(RegraNegocioException erro) {
    return ResponseEntity.status(erro.getStatus()).body(new ErroResponse(erro.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException erro) {
    String mensagem = erro.getBindingResult().getAllErrors().stream()
        .map(ObjectError::getDefaultMessage)
        .findFirst()
        .orElse("Dados inválidos.");
    return ResponseEntity.badRequest().body(new ErroResponse(mensagem));
  }

  // Dois pedidos ao mesmo tempo podem passar pela verificação do serviço; nesse caso quem barra é o banco
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErroResponse> integridade() {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErroResponse("Este registro já existe ou está em uso."));
  }
}
