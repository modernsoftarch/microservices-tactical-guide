package com.tmsa.brujula.recaudo.infraestructura.web;

import com.tmsa.brujula.recaudo.dominio.ClaveReutilizadaException;
import com.tmsa.brujula.recaudo.dominio.CobroEnProcesoException;
import com.tmsa.brujula.recaudo.dominio.PasarelaNoDisponibleException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce las excepciones de idempotencia a códigos HTTP que un cliente sabe reintentar. */
@RestControllerAdvice
class ManejadorDeErrores {

    @ExceptionHandler(CobroEnProcesoException.class)
    ProblemDetail enProceso(CobroEnProcesoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(ClaveReutilizadaException.class)
    ProblemDetail claveReutilizada(ClaveReutilizadaException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(422), e.getMessage());
    }

    @ExceptionHandler(PasarelaNoDisponibleException.class)
    ProblemDetail pasarelaCaida(PasarelaNoDisponibleException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail datosInvalidos(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
