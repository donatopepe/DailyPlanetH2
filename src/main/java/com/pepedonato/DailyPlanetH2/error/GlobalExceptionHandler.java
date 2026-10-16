package com.pepedonato.DailyPlanetH2.error;

import jakarta.servlet.http.HttpServletRequest;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Gestione centralizzata degli errori applicativi.
 *
 * <p>Prima di questa classe gli errori finivano nella whitelabel page di Spring Boot o in
 * una eccezione non gestita: qui vengono registrati con SLF4J (senza dati sensibili) e
 * mostrati all'utente nella pagina {@code error/error}.</p>
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoResource(NoResourceFoundException e, HttpServletRequest request, Model model) {
        log.debug("Risorsa non trovata: {} {}", request.getMethod(), request.getRequestURI());
        return popola(model, request, HttpStatus.NOT_FOUND, "Pagina non trovata", "La pagina richiesta non esiste.");
    }

    @ExceptionHandler({NoSuchElementException.class, IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(RuntimeException e, HttpServletRequest request, Model model) {
        log.warn("Richiesta non valida: {} {}", request.getMethod(), request.getRequestURI(), e);
        return popola(model, request, HttpStatus.NOT_FOUND, "Risorsa non trovata", "La risorsa richiesta non esiste.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleDataIntegrity(DataIntegrityViolationException e, HttpServletRequest request, Model model) {
        log.warn("Violazione di vincolo dati: {} {}", request.getMethod(), request.getRequestURI(), e);
        return popola(model, request, HttpStatus.CONFLICT, "Operazione non consentita",
                "L'operazione non rispetta i vincoli dei dati (per esempio un valore già presente).");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneric(Exception e, HttpServletRequest request, Model model) {
        log.error("Errore non gestito: {} {}", request.getMethod(), request.getRequestURI(), e);
        return popola(model, request, HttpStatus.INTERNAL_SERVER_ERROR, "Errore interno",
                "Si è verificato un errore imprevisto. L'errore è stato registrato.");
    }

    private String popola(Model model, HttpServletRequest request, HttpStatus status, String titolo, String messaggio) {
        model.addAttribute("status", status.value());
        model.addAttribute("error", titolo);
        model.addAttribute("message", messaggio);
        model.addAttribute("path", request.getRequestURI());
        return "error/error";
    }
}
