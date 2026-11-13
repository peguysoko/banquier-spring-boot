import { useApi } from "../useApi";
import { formatAmount } from "../format";

// Une petite pastille verte "OK" ou rouge selon le résultat d'un contrôle
function Badge({ ok, okText = "OK", koText = "À VÉRIFIER" }) {
  return <span className={ok ? "badge ok" : "badge ko"}>{ok ? okText : koText}</span>;
}

export default function Reconciliation({ authHeader }) {
  const { data, error } = useApi("/api/reconciliation", authHeader);

  if (error) return <p className="error">{error}</p>;
  if (!data) return <p>Calcul en cours…</p>;

  // On range les 5 contrôles dans des variables au nom lisible
  const c1 = data.controle1_soldes_contre_journal;
  const c2 = data.controle2_virements_complets;
  const c3 = data.controle3_debits_egal_credits;
  const c4 = data.controle4_tracabilite;
  const c5 = data.controle5_doublons_suspects;

  return (
    <div>
      <h3>
        Statut de l'argent : <Badge ok={data.statut_argent === "COHERENT"} okText="COHÉRENT" koText="ANOMALIES" />
      </h3>

      <ul className="controls">
        <li>
          <Badge ok={c1.ok} /> <strong>1. Soldes contre journal</strong>
          {c1.ecarts.map((gap) => (
            <p key={gap.account_id} className="detail">
              {gap.account_number} : le compte indique {formatAmount(gap.account_balance)}, le journal {formatAmount(gap.ledger_balance)}
            </p>
          ))}
        </li>

        <li>
          <Badge ok={c2.ok} /> <strong>2. Virements complets</strong>
          {c2.anomalies.map((item) => (
            <p key={item.transfer_id} className="detail">Virement {item.transfer_id} : écritures incomplètes</p>
          ))}
        </li>

        <li>
          <Badge ok={c3.ok} okText="ÉGAUX" koText="ÉCART" /> <strong>3. Débits = crédits</strong>
          <p className="detail">Débits {formatAmount(c3.total_debits)} / Crédits {formatAmount(c3.total_credits)}</p>
        </li>

        <li>
          <Badge ok={c4.ok} /> <strong>4. Traçabilité</strong>
          {c4.virements_sans_audit.length > 0 && (
            <p className="detail">
              Virements sans ligne d'audit : {c4.virements_sans_audit.map((v) => v.transfer_id).join(", ")}
            </p>
          )}
        </li>

        <li>
          <Badge ok={c5.paires.length === 0} koText="ALERTE" /> <strong>5. Doublons suspects</strong>
          {c5.paires.map((pair) => (
            <p key={pair.transfer_id_1 + "-" + pair.transfer_id_2} className="detail">
              Virements {pair.transfer_id_1} et {pair.transfer_id_2} : {formatAmount(pair.amount)},
              {" "}{pair.seconds_apart} seconde(s) d'écart
            </p>
          ))}
        </li>
      </ul>
    </div>
  );
}