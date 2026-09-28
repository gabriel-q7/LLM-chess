import { Move } from './models/move.model';
import { describeMove, movedPiece } from './pieces';

const move = (overrides: Partial<Move>): Move => ({
  ply: 1,
  moveNumber: 1,
  color: 'WHITE',
  from: 'e2',
  to: 'e4',
  promotion: null,
  san: 'e4',
  fen: '',
  ...overrides,
});

describe('pieces', () => {
  it('reads the moved piece from SAN', () => {
    expect(movedPiece('e4')).toBe('p');
    expect(movedPiece('exd5')).toBe('p');
    expect(movedPiece('Nf3')).toBe('n');
    expect(movedPiece('Bxc6+')).toBe('b');
    expect(movedPiece('Qh4#')).toBe('q');
    expect(movedPiece('O-O')).toBe('k');
    expect(movedPiece('O-O-O')).toBe('k');
    expect(movedPiece('e8=Q')).toBe('p');
  });

  it('describes moves in words', () => {
    expect(describeMove(move({ san: 'Nf3', from: 'g1', to: 'f3' }))).toBe('White knight g1 → f3');
    expect(describeMove(move({ san: 'Bxc6+', from: 'b5', to: 'c6', color: 'BLACK' }))).toBe(
      'Black bishop b5 → c6, capture, check',
    );
    expect(describeMove(move({ san: 'O-O', from: 'e1', to: 'g1' }))).toBe('White king e1 → g1, kingside castling');
    expect(describeMove(move({ san: 'exd8=N#', from: 'e7', to: 'd8', promotion: 'n' }))).toBe(
      'White pawn e7 → d8, capture, promotes to knight, checkmate',
    );
  });
});
