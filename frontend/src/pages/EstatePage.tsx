import { Link, useParams } from 'react-router-dom';
import CreditorsPanel from '../components/CreditorsPanel';
import AssetsPanel from '../components/AssetsPanel';

// Manage the debtor's financial inventory (creditors and assets) for one case.
// Presentation + CRUD only — no legal/eligibility logic; ownership is enforced by the backend.
export default function EstatePage() {
  const { applicationId } = useParams<{ applicationId: string }>();
  if (!applicationId) return null;

  return (
    <section>
      <div className="page-head">
        <h2>Кредиторы и имущество</h2>
        <Link to={`/applications/${applicationId}/questionnaire`}>К анкете</Link>
      </div>
      <CreditorsPanel applicationId={applicationId} />
      <AssetsPanel applicationId={applicationId} />
    </section>
  );
}
