package com.banquier.bank.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

// "record" = une petite boîte qui sert seulement à transporter des données reçues.
// DTO = "Data Transfer Object" : la forme de ce que le client nous envoie.
public record TransferRequest(
        @NotNull Long fromAccountId,            // @NotNull : obligatoire
        @NotNull Long toAccountId,              // @NotNull : obligatoire
        @NotNull @Positive BigDecimal amount    // @Positive : doit être supérieur à 0
) {
}