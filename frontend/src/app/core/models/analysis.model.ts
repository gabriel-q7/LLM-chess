import { AiTrace } from './ai-context.model';

/** Engine fields the game's difficulty hides are null. */
export interface Analysis {
  bestMove: string | null;
  /** Pawns from White's point of view. */
  evaluation: number | null;
  depth: number;
  /** Moves to forced mate; positive when White mates. */
  mateIn: number | null;
  explanation: string;
  trace: AiTrace | null;
}

export interface ChatRequest {
  question: string;
}

export interface ChatResponse {
  answer: string;
  trace: AiTrace | null;
}

export interface ApiError {
  code: string;
  message: string;
}
