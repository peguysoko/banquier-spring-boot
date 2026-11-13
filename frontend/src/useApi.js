import { useEffect, useState } from "react";   // les deux outils de React expliqués plus haut
import { apiGet } from "./api";

// Un "hook" personnalisé : une fonction réutilisable qui va chercher des données et renvoie { data, error }.
export function useApi(path, authHeader) {
  // La mémoire de ce hook : les données reçues (null = pas encore reçues) et un éventuel message d'erreur
  const [state, setState] = useState({ data: null, error: "" });

  // useEffect : ce code s'exécute quand le composant apparaît (et si "path" ou "authHeader" changent)
  useEffect(() => {
    let cancelled = false;   // devient true si l'écran est fermé avant que la réponse n'arrive

    apiGet(path, authHeader)
      .then((data) => { if (!cancelled) setState({ data, error: "" }); })            // succès : on garde les données
      .catch((err) => { if (!cancelled) setState({ data: null, error: err.message }); }); // échec : on garde le message

    // Cette fonction s'exécute quand l'écran disparaît : on ne met plus rien à jour
    return () => { cancelled = true; };
  }, [path, authHeader]);

  return state;
}