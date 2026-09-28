import { Difficulty } from './game.model';

/** What Stockfish received and returned for the computer's latest move. */
export interface ComputerMoveContext {
  ply: number;
  san: string;
  /** Position sent to Stockfish. */
  fen: string;
  difficulty: Difficulty;
  engine: {
    move: string;
    strength: { skillLevel: number; depth: number };
    depth: number;
    /** Pawns from White's point of view. */
    evaluation: number;
    mateIn: number | null;
    principalVariation: string[];
    uciCommands: string[];
    finalInfo: string | null;
    bestMoveLine: string | null;
  };
  principalVariationSan: string[];
}

export interface Disclosure {
  bestMove: boolean;
  pieceHint: boolean;
  continuation: boolean;
  numericEvaluation: boolean;
  playerMate: 'EXACT' | 'EXISTS' | 'HIDDEN';
}

export interface LayaQuestion {
  type: 'choice' | 'score' | 'noul';
  instructions: string;
  criteria: Record<string, string> | null;
}

export interface LayaDecision {
  choice: string;
  probability: number;
  /** Whether the probability reached the confidence threshold, so the reply relied on it. */
  used: boolean;
  probabilities: Record<string, number>;
}

export interface LayaExchange {
  model: string;
  /** Text Laya read. */
  state: string;
  questions: Record<string, LayaQuestion>;
  decisions: Record<string, LayaDecision>;
}

/** Everything the assistant received and decided for one reply. */
export interface AiTrace {
  disclosure: Disclosure;
  hiddenFacts: string[];
  exchanges: LayaExchange[];
}
