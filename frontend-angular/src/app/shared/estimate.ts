// Indicative browser preview only. The backend remains authoritative for financial decisions.
export function monthlyEstimate(principal: number, annualRate: number, months: number): number | null {
  if (![principal, annualRate, months].every(Number.isFinite) || principal <= 0 || annualRate < 0 || annualRate > 100 || months < 1 || months > 360 || !Number.isInteger(months)) return null;
  const monthly = Math.expm1(Math.log1p(annualRate / 100) / 12);
  return monthly === 0 ? principal / months : principal * monthly / -Math.expm1(-months * Math.log1p(monthly));
}
