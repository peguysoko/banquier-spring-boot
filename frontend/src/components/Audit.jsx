import { useApi } from "../useApi";
import { formatDate } from "../format";

export default function Audit({ authHeader }) {
  const { data, error } = useApi("/api/audit", authHeader);

  if (error) return <p className="error">{error}</p>;
  if (!data) return <p>Chargement…</p>;

  return (
    <table>
      <thead>
        <tr><th>N°</th><th>Date</th><th>Utilisateur</th><th>Action</th><th>Résultat</th><th>Détails</th></tr>
      </thead>
      <tbody>
        {data.map((entry) => (
          <tr key={entry.id}>
            <td>{entry.id}</td>
            <td>{formatDate(entry.occurredAt)}</td>
            <td>{entry.username}</td>
            <td>{entry.action}</td>
            {/* La classe change selon le résultat : vert si réussi, rouge si refusé */}
            <td><span className={entry.outcome === "SUCCESS" ? "badge ok" : "badge ko"}>{entry.outcome}</span></td>
            <td>{entry.details}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}