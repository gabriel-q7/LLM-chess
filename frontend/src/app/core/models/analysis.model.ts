export interface Analysis {
  bestMove: string | null;
  /** Pawns from White's point of view. */
  evaluation: number;
  depth: number;
  /** Moves to forced mate; positive when White mates. */
  mateIn: number | null;
  explanation: string;
}

export interface ChatRequest {
  question: string;
}

export interface ChatResponse {
  answer: string;
  bestMove: string | null;
  evaluation: number;
  depth: number;
  mateIn: number | null;
}

export interface ApiError {
  code: string;
  message: string;
}
