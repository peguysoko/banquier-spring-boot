# Banquier — mini-système bancaire (Spring Boot, MySQL, Python)

Projet d'étude qui reproduit les garanties d'un système bancaire : **fiabilité, traçabilité et sécurité**.

## Ce que le projet démontre

- **Virements idempotents** : une clé unique par demande ; un même virement envoyé deux fois n'est exécuté qu'une fois.
- **Tout ou rien** : un virement est une transaction (débit, crédit, reçu et journal) ; un refus ne laisse aucune trace dans les soldes.
- **Journal comptable** : chaque virement écrit un débit et un crédit avec le solde après mouvement.
- **Rôles et permissions** : `CLIENT`, `CAISSIER`, `AUDITEUR`, `ADMIN` ; un client ne voit que ses propres comptes ; tout ce qui n'est pas explicitement autorisé est refusé.
- **Audit non modifiable** : qui a fait quoi, y compris les refus ; les modifications et suppressions sont bloquées par MySQL.
- **Réconciliation** : soldes contre journal, virements complets, débits = crédits, traçabilité, alerte sur les doublons suspects.
- **Annulation par écriture inverse** : on n'efface jamais une écriture ; une seule annulation par virement, réservée à l'ADMIN.
- **Contrôleur indépendant en Python** : un script lit le rapport de réconciliation et rédige un compte rendu.

## Architecture

Controller → Service → Repository → MySQL.
Java 21, Spring Boot 4, Spring Security (HTTP Basic, mots de passe hachés BCrypt), JPA/Hibernate, MySQL 8, Python 3 (bibliothèque standard).

## Les accès de l'API

| Méthode et adresse                                | Rôles autorisés                          |
| ------------------------------------------------- | ---------------------------------------- |
| `POST /api/transfers` (en-tête `Idempotency-Key`) | CAISSIER, ADMIN                          |
| `POST /api/transfers/{id}/reverse`                | ADMIN                                    |
| `GET /api/accounts`, `GET /api/accounts/{id}`     | tous (un CLIENT ne voit que ses comptes) |
| `GET /api/audit`                                  | AUDITEUR, ADMIN                          |
| `GET /api/reconciliation`                         | AUDITEUR, ADMIN                          |

## Lancer le projet

1. Créer la base : exécuter `database/schema.sql` dans MySQL.
2. Définir les variables d'environnement (jamais de mot de passe dans le code) :
   - `DB_PASSWORD` : mot de passe MySQL (`DB_USERNAME` vaut `root` par défaut)
   - `DEMO_PASSWORD` : mot de passe commun des utilisateurs de démonstration
3. Démarrer : `mvn spring-boot:run`
4. Lier les comptes aux clients (une fois, après le premier démarrage) :
   `UPDATE accounts SET owner_user_id = (SELECT id FROM users WHERE username = 'client1') WHERE id = 1;`
   puis la même chose pour `client2` et le compte 2.
5. Rapport Python : définir `BANK_PASSWORD`, puis `python python/rapport_reconciliation.py`.

## Limites connues et améliorations prévues

- Les refus de permission (403) du filtre de sécurité ne sont pas écrits dans l'audit.
- Un rappel idempotent est journalisé comme un succès ; il pourrait être marqué `REPLAY`.
- Quelques virements faits avant la mise en place de l'audit n'ont pas de ligne d'audit : exception historique connue, signalée par la réconciliation.
- L'authentification HTTP Basic est adaptée à une démonstration ; une version de production utiliserait des jetons.
- Un administrateur de la base peut encore supprimer les verrous de l'audit ; en production, le compte de l'application n'aurait pas ce droit.
