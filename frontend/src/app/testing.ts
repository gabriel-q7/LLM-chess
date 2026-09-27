import { Game } from './core/models/game.model';
import { LegalMove } from './core/models/move.model';

export const START_FEN = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';

export function gameFixture(overrides: Partial<Game> = {}): Game {
  const legalMoves: LegalMove[] = [
    { from: 'e2', to: 'e3', promotion: null },
    { from: 'e2', to: 'e4', promotion: null },
    { from: 'g1', to: 'f3', promotion: null },
  ];
  return {
    id: 'game-1',
    fen: START_FEN,
    status: 'PLAYING',
    turn: 'WHITE',
    playerColor: 'WHITE',
    difficulty: 'MEDIUM',
    check: false,
    winner: null,
    drawReason: null,
    moves: [],
    legalMoves,
    ...overrides,
  };
}
