# -*- coding: utf-8 -*-
"""
Rapport de réconciliation : interroge la banque, lit le rapport des contrôles,
écrit un compte rendu lisible et le sauvegarde dans un fichier.
"""

# "import" = on emprunte des outils déjà écrits. Tous sont fournis avec Python : rien à installer.
import base64                      # pour fabriquer l'en-tête de connexion (identifiant:motdepasse)
import json                        # pour lire la réponse de la banque, qui est au format JSON
import os                          # pour lire les variables d'environnement (le mot de passe)
import sys                         # pour arrêter le programme avec un code de sortie
import urllib.request              # pour envoyer une demande web à la banque
import urllib.error                # pour attraper les erreurs réseau (banque éteinte, mauvais mot de passe)
from datetime import datetime      # pour écrire la date et l'heure du rapport
from pathlib import Path           # pour manipuler les dossiers et fichiers proprement

# L'adresse de la banque : le même localhost:8080 que dans PowerShell
BASE_URL = "http://localhost:8080"

# Les virements que l'on sait faits AVANT la mise en place de l'audit (exception historique connue).
# Un ensemble {...} en Python = une collection de valeurs, pratique pour demander "est-ce que X en fait partie ?".
KNOWN_UNAUDITED = {3, 4, 5, 6}


def fetch_report():
    """Va chercher le rapport de réconciliation auprès de la banque."""

    # os.environ.get(...) lit une variable d'environnement. Le 2e argument est la valeur par défaut.
    user = os.environ.get("BANK_USER", "auditeur1")
    password = os.environ.get("BANK_PASSWORD")

    # Si le mot de passe n'a pas été fourni, on s'arrête avec un message clair.
    if not password:
        print("Erreur : définis d'abord le mot de passe, par exemple :")
        print('  $env:BANK_PASSWORD = "ton_mot_de_passe"')
        sys.exit(2)                # sys.exit(2) = on quitte le programme avec le code 2 (= problème de configuration)

    # On fabrique l'en-tête "Basic ..." : "identifiant:motdepasse" converti en Base64.
    # .encode("utf-8") transforme le texte en octets, .decode("ascii") retransforme le résultat en texte.
    token = base64.b64encode(f"{user}:{password}".encode("utf-8")).decode("ascii")

    # On prépare la demande : l'adresse + l'en-tête qui contient notre badge.
    request = urllib.request.Request(
        BASE_URL + "/api/reconciliation",
        headers={"Authorization": "Basic " + token},
    )

    # try / except = "essaie ceci ; si ça échoue, fais cela au lieu de planter"
    try:
        # "with" ouvre la connexion et la referme toute seule à la fin. timeout=15 = on abandonne après 15 secondes.
        with urllib.request.urlopen(request, timeout=15) as response:
            # On lit la réponse en UTF-8 (ce qui garde les accents corrects), puis json.loads la transforme
            # en objets Python : des dictionnaires { } et des listes [ ].
            return json.loads(response.read().decode("utf-8"))

    except urllib.error.HTTPError as error:
        # La banque a répondu avec un code d'erreur (401 = mauvais badge, 403 = pas le droit...)
        print(f"Erreur : la banque a répondu avec le code {error.code}.")
        sys.exit(2)

    except urllib.error.URLError:
        # Impossible de joindre la banque : application éteinte ou mauvaise adresse
        print(f"Erreur : impossible de joindre la banque sur {BASE_URL}. L'application est-elle démarrée ?")
        sys.exit(2)


def money(value):
    """Écrit un montant lisible : 52000.0 devient '52 000.00'."""
    # {value:,.2f} = séparateur de milliers (une virgule) et 2 chiffres après la virgule ; on remplace la virgule par une espace.
    return f"{value:,.2f}".replace(",", " ")


def build_lines(report):
    """Transforme le rapport brut en une liste de lignes de texte lisibles."""

    lines = []                                         # on ajoute les lignes au fur et à mesure
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S") # la date et l'heure, au format 2026-11-11 10:30:00

    lines.append("RAPPORT DE RÉCONCILIATION")
    lines.append(f"Généré le : {now}")
    lines.append("")
    lines.append(f"Statut de l'argent : {report['statut_argent']}")
    lines.append("")

    # CONTRÔLE 1
    c1 = report["controle1_soldes_contre_journal"]
    lines.append(f"1. Soldes contre journal : {'OK' if c1['ok'] else 'ÉCARTS DÉTECTÉS'}")
    for gap in c1["ecarts"]:                           # "for" = on répète pour chaque élément de la liste
        lines.append(
            f"   - {gap['account_number']} : le compte indique {money(gap['account_balance'])}, "
            f"le journal indique {money(gap['ledger_balance'])}"
        )

    # CONTRÔLE 2
    c2 = report["controle2_virements_complets"]
    lines.append(f"2. Virements complets : {'OK' if c2['ok'] else 'ANOMALIES'}")
    for item in c2["anomalies"]:
        lines.append(f"   - virement {item['transfer_id']} ({item['idempotency_key']}) : écritures incomplètes")

    # CONTRÔLE 3
    c3 = report["controle3_debits_egal_credits"]
    lines.append(
        f"3. Débits = crédits : {'OK' if c3['ok'] else 'ÉCART'} "
        f"(débits {money(c3['total_debits'])}, crédits {money(c3['total_credits'])})"
    )

    # CONTRÔLE 4 : on sépare les exceptions CONNUES des problèmes NOUVEAUX
    c4 = report["controle4_tracabilite"]
    # Liste en compréhension : "garde les virements dont le numéro n'est PAS dans les exceptions connues"
    new_unaudited = [v for v in c4["virements_sans_audit"] if v["transfer_id"] not in KNOWN_UNAUDITED]
    known_unaudited = [v for v in c4["virements_sans_audit"] if v["transfer_id"] in KNOWN_UNAUDITED]
    lines.append(
        f"4. Traçabilité : {'OK' if not new_unaudited else 'NOUVEAUX VIREMENTS SANS AUDIT'} "
        f"({len(known_unaudited)} exception(s) historique(s) connue(s))"
    )
    for item in new_unaudited:
        lines.append(f"   - NOUVEAU : virement {item['transfer_id']} ({item['idempotency_key']}) sans ligne d'audit")

    # CONTRÔLE 5 : une alerte, pas une erreur
    c5 = report["controle5_doublons_suspects"]
    lines.append(f"5. Doublons suspects : {len(c5['paires'])} paire(s) à vérifier")
    for pair in c5["paires"]:
        lines.append(
            f"   - virements {pair['transfer_id_1']} et {pair['transfer_id_2']} : "
            f"{money(pair['amount'])} du compte {pair['from_account_id']} vers {pair['to_account_id']}, "
            f"{pair['seconds_apart']} seconde(s) d'écart"
        )

    return lines, bool(new_unaudited)


def save_report(lines):
    """Sauvegarde le rapport dans un fichier daté, dans le dossier 'rapports'."""

    # Path(__file__).parent = le dossier où se trouve ce script ; "/" sert à ajouter un sous-dossier
    folder = Path(__file__).parent / "rapports"
    folder.mkdir(exist_ok=True)                        # on crée le dossier s'il n'existe pas déjà

    file_name = "reconciliation_" + datetime.now().strftime("%Y-%m-%d_%H%M%S") + ".txt"
    path = folder / file_name
    # encoding="utf-8" : les accents sont bien enregistrés
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return path


def main():
    report = fetch_report()                            # 1) on interroge la banque
    lines, has_new_unaudited = build_lines(report)     # 2) on rédige le compte rendu

    print("\n".join(lines))                            # 3) on l'affiche à l'écran...
    path = save_report(lines)                          #    ...et on le sauvegarde
    print(f"\nRapport sauvegardé : {path}")

    # Code de sortie : 0 = tout va bien, 1 = il y a un problème à traiter.
    # Un programme de planification (ou un autre script) peut s'en servir pour déclencher une alerte.
    problem = report["statut_argent"] != "COHERENT" or has_new_unaudited
    sys.exit(1 if problem else 0)


# Cette condition veut dire : "lance main() seulement si on exécute CE fichier directement"
if __name__ == "__main__":
    main()