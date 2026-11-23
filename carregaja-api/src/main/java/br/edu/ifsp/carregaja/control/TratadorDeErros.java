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

  // Parâmetro da URL com formato inválido, como uma data fora de aaaa-mm-dd
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErroResponse> parametroInvalido(MethodArgumentTypeMismatchException erro) {
    return ResponseEntity.badRequest().body(new ErroResponse("O parâmetro '" + erro.getName() + "' é inválido."));
  }

  // Dois pedidos ao mesmo tempo podem passar pela verificação do serviço; nesse caso quem barra é o banco
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErroResponse> integridade() {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErroResponse("Este registro já existe ou está em uso."));
  }
}
