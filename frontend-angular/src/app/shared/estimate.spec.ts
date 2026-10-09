import { monthlyEstimate } from './estimate';
describe('Indicative mortgage calculation', () => {
  it('matches the documented French amortization example', () => {
    expect(monthlyEstimate(240000, 9, 240)).toBeCloseTo(2105.43, 2);
  });
  it('handles a zero interest rate without division by zero', () => {
    expect(monthlyEstimate(120000, 0, 120)).toBe(1000);
  });
  it('does not show a payment for invalid or incomplete scenarios', () => {
    for (const args of [[0,9,240], [10000,-1,240], [10000,9,0], [10000,9,1.5], [10000,9,361], [NaN,9,240], [10000,Infinity,240]]) {
      expect(monthlyEstimate(args[0],args[1],args[2])).toBeNull();
    }
  });
});
