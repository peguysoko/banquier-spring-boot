import { useState } from "react";
import { makeAuthHeader, apiGet } from "./api";
import Accounts from "./components/Accounts";
import Reconciliation from "./components/Reconciliation";
import Audit from "./components/Audit";
import "./App.css";

function App() {
  // La mémoire de l'application
  const [session, setSession] = useState(null);      // null = personne n'est connecté
  const [tab, setTab] = useState("accounts");         // l'écran affiché
  const [username, setUsername] = useState("");       // ce que la personne tape dans le formulaire
  const [password, setPassword] = useState("");
  const [loginError, setLoginError] = useState("");
  const [loading, setLoading] = useState(false);

  // Appelée quand on envoie le formulaire de connexion
  async function handleLogin(event) {
    event.preventDefault();      // empêche le navigateur de recharger la page
    setLoginError("");
    setLoading(true);

    const authHeader = makeAuthHeader(username, password);
    try {
      // Pour vérifier le badge, on demande la liste des comptes : tous les rôles y ont droit.
      await apiGet("/api/accounts", authHeader);
      // Succès : on garde le badge EN MÉMOIRE (pas dans le navigateur) et on efface le mot de passe saisi
      setSession({ username, authHeader });
      setPassword("");
    } catch (error) {
      setLoginError(error.message);
    } finally {
      setLoading(false);          // dans tous les cas, on arrête l'indicateur de chargement
    }
  }

  // Se déconnecter = oublier le badge
  function handleLogout() {
    setSession(null);
    setUsername("");
    setPassword("");
    setTab("accounts");
  }

  // ÉCRAN 1 : personne n'est connecté -> le formulaire de connexion
  if (!session) {
    return (
      <main className="card login">
        <h1>Banquier</h1>
        <p>Connectez-vous avec votre compte.</p>
        <form onSubmit={handleLogin}>
          <label>
            Identifiant
            <input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" required />
          </label>
          <label>
            Mot de passe
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" required />
          </label>
          {loginError && <p className="error">{loginError}</p>}
          <button type="submit" disabled={loading}>{loading ? "Connexion…" : "Se connecter"}</button>
        </form>
      </main>
    );
  }

  // ÉCRAN 2 : connecté -> la barre du haut, les onglets et l'écran choisi
  return (
    <main className="card">
      <header>
        <h1>Banquier</h1>
        <div>
          Connecté : <strong>{session.username}</strong>{" "}
          <button className="link" onClick={handleLogout}>Se déconnecter</button>
        </div>
      </header>

      <nav>
        <button className={tab === "accounts" ? "tab active" : "tab"} onClick={() => setTab("accounts")}>Comptes</button>
        <button className={tab === "reconciliation" ? "tab active" : "tab"} onClick={() => setTab("reconciliation")}>Réconciliation</button>
        <button className={tab === "audit" ? "tab active" : "tab"} onClick={() => setTab("audit")}>Audit</button>
      </nav>

      {/* On affiche seulement l'écran choisi. Si le rôle n'a pas le droit, la banque répond 403 et l'écran l'explique. */}
      {tab === "accounts" && <Accounts authHeader={session.authHeader} />}
      {tab === "reconciliation" && <Reconciliation authHeader={session.authHeader} />}
      {tab === "audit" && <Audit authHeader={session.authHeader} />}
    </main>
  );
}

export default App;