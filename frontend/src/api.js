// L'adresse de la banque (le backend Java)
const BASE_URL = "http://localhost:8080";

// Fabrique l'en-tête de connexion "Basic ..." à partir de l'identifiant et du mot de passe.
// btoa = convertit un texte en Base64 (ce n'est PAS un cryptage, juste une façon de l'écrire pour l'envoyer).
export function makeAuthHeader(username, password) {
  return "Basic " + btoa(`${username}:${password}`);
}

// Envoie une demande GET à la banque et renvoie la réponse sous forme d'objets JavaScript.
// "async" = cette fonction attend des réponses lentes (le réseau) ; "await" = "attends la réponse ici".
export async function apiGet(path, authHeader) {
  let response;

  // try / catch = "essaie ceci ; si ça échoue, fais cela au lieu de planter"
  try {
    response = await fetch(BASE_URL + path, {
      headers: { Authorization: authHeader },   // on présente notre badge
    });
  } catch {
    // fetch échoue (sans réponse) quand la banque est éteinte ou injoignable
    throw new Error("Impossible de joindre la banque. L'application Java est-elle démarrée ?");
  }

  // La banque a répondu : on traduit les codes d'erreur en messages clairs
  if (response.status === 401) throw new Error("Identifiant ou mot de passe incorrect.");
  if (response.status === 403) throw new Error("Accès refusé : votre rôle ne permet pas de voir cet écran.");
  if (!response.ok) throw new Error(`Erreur inattendue (code ${response.status}).`);

  // .json() transforme le texte reçu en objets utilisables (les accents sont gardés : le format est UTF-8)
  return response.json();
}