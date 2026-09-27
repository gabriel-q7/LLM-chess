import { LegalMove, Move } from './move.model';

export type Color = 'WHITE' | 'BLACK';

/** Sets both the computer's strength and how much the AI assistant reveals. */
export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD';

export type GameStatus = 'PLAYING' | 'CHECKMATE' | 'STALEMATE' | 'DRAW';

export type DrawReason = 'STALEMATE' | 'REPETITION' | 'INSUFFICIENT_MATERIAL' | 'FIFTY_MOVE_RULE';

export interface Game {
  id: string;
  fen: string;
  status: GameStatus;
  turn: Color;
  playerColor: Color;
  difficulty: Difficulty;
  check: boolean;
  winner: Color | null;
  drawReason: DrawReason | null;
  moves: Move[];
  /** The player's legal moves, computed by the backend. Empty when it is not the player's turn. */
  legalMoves: LegalMove[];
}

export interface CreateGameRequest {
  playerColor: Color;
  difficulty: Difficulty;
}
