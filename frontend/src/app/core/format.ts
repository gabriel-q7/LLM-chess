/** Formats an engine score, e.g. "+0.42" or "#3" / "#-2" for forced mates. */
export function formatEvaluation(evaluation: number, mateIn: number | null): string {
  if (mateIn !== null) {
    return mateIn >= 0 ? `#${mateIn}` : `#-${Math.abs(mateIn)}`;
  }
  return `${evaluation >= 0 ? '+' : ''}${evaluation.toFixed(2)}`;
}
