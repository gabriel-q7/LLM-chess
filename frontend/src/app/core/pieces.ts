import { Move } from './models/move.model';

export type PieceType = 'k' | 'q' | 'r' | 'b' | 'n' | 'p';

// Solid glyphs for both colours; U+FE0E asks for text rather than emoji rendering.
export const PIECE_GLYPHS: Record<PieceType, string> = {
  k: '♚︎',
  q: '♛︎',
  r: '♜︎',
  b: '♝︎',
  n: '♞︎',
  p: '♟︎',
};

export const PIECE_NAMES: Record<PieceType, string> = {
  k: 'king',
  q: 'queen',
  r: 'rook',
  b: 'bishop',
  n: 'knight',
  p: 'pawn',
};

/** Piece that made a move, read from its SAN (castling is a king move; no letter means a pawn). */
export function movedPiece(san: string): PieceType {
  if (san.startsWith('O-O')) {
    return 'k';
  }
  const letter = san.charAt(0);
  return 'KQRBN'.includes(letter) ? (letter.toLowerCase() as PieceType) : 'p';
}

/** Full description of a move, e.g. "White knight g1 → f3, capture, check". */
export function describeMove(move: Move): string {
  const san = move.san;
  const color = move.color === 'WHITE' ? 'White' : 'Black';
  const details: string[] = [];
  if (san.startsWith('O-O-O')) {
    details.push('queenside castling');
  } else if (san.startsWith('O-O')) {
    details.push('kingside castling');
  }
  if (san.includes('x')) {
    details.push('capture');
  }
  if (move.promotion) {
    details.push(`promotes to ${PIECE_NAMES[move.promotion]}`);
  }
  if (san.endsWith('#')) {
    details.push('checkmate');
  } else if (san.endsWith('+')) {
    details.push('check');
  }
  const base = `${color} ${PIECE_NAMES[movedPiece(san)]} ${move.from} → ${move.to}`;
  return details.length ? `${base}, ${details.join(', ')}` : base;
}
