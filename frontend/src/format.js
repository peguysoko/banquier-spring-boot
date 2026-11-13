// Écrit un montant lisible : 958000 devient "958 000,00 CDF"
export function formatAmount(value, currency = "CDF") {
  // Intl.NumberFormat = l'outil du navigateur pour écrire les nombres à la française
  return new Intl.NumberFormat("fr-FR", { minimumFractionDigits: 2 }).format(value) + " " + currency;
}

// Écrit une date lisible. La banque envoie l'heure en UTC SANS le "Z" final :
// on l'ajoute, sinon le navigateur croirait que c'est l'heure locale et décalerait d'une heure.
export function formatDate(isoText) {
  return new Date(isoText + "Z").toLocaleString("fr-FR");
}