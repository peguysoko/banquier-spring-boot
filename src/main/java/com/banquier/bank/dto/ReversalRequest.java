package com.banquier.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Le formulaire d'annulation : un motif OBLIGATOIRE (pour l'audit), de 200 caractères au maximum
public record ReversalRequest(
        @NotBlank @Size(max = 200) String reason
) {
}