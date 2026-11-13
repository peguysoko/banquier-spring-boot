import { useApi } from "../useApi";
import { formatAmount } from "../format";

// Un composant reçoit des "props" (des informations venues de l'extérieur) entre accolades.
export default function Accounts({ authHeader }) {
  // On va chercher les comptes : un CLIENT reçoit seulement les siens (c'est Java qui filtre)
  const { data, error } = useApi("/api/accounts", authHeader);

  // Trois cas d'affichage : erreur, chargement en cours, liste vide
  if (error) return <p className="error">{error}</p>;
  if (!data) return <p>Chargement…</p>;
  if (data.length === 0) return <p>Aucun compte à afficher.</p>;

  return (
    <table>
      <thead>
        <tr><th>Compte</th><th>Titulaire</th><th>Statut</th><th className="right">Solde</th></tr>
      </thead>
      <tbody>
        {/* .map = pour chaque compte de la liste, on fabrique une ligne de tableau.
            "key" aide React à reconnaître chaque ligne. */}
        {data.map((account) => (
          <tr key={account.id}>
            <td>{account.accountNumber}</td>
            <td>{account.ownerName}</td>
            <td>{account.status}</td>
            <td className="right">{formatAmount(account.balance, account.currency)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}