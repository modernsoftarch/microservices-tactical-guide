package com.tmsa.brujula.cotizacion.infraestructura.web;

import com.tmsa.brujula.cotizacion.dominio.CotizacionNoEncontradaException;
import com.tmsa.brujula.cotizacion.dominio.TransicionInvalidaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce excepciones del dominio a respuestas RFC 9457 (Problem Details). */
@RestControllerAdvice
class ManejadorDeErrores {

    @ExceptionHandler(CotizacionNoEncontradaException.class)
    ProblemDetail noEncontrada(CotizacionNoEncontradaException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(TransicionInvalidaException.class)
    ProblemDetail transicionInvalida(TransicionInvalidaException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail datosInvalidos(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(422), e.getMessage());
    }
}
