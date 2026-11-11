package com.banquier.bank.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

// @RestControllerAdvice = "le service des réclamations" : il attrape les erreurs lancées
// n'importe où dans nos guichets et les transforme en réponses propres.
@RestControllerAdvice
public class ApiExceptionHandler {

    // Demande incorrecte (compte introuvable, comptes identiques...) -> code 400
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erreur", e.getMessage()));
    }

    // Règle de la banque violée (solde insuffisant, clé réutilisée...) -> code 409 "conflit"
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> conflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erreur", e.getMessage()));
    }

    // Filet de sécurité : si deux demandes avec la MÊME clé arrivent exactement en même temps,
    // c'est la contrainte UNIQUE de MySQL (celle de l'étape 1) qui bloque la deuxième.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> duplicate(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("erreur", "Demande déjà en cours de traitement, réessayez dans un instant"));
    }
}