import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../models/analysis.model';

const MESSAGES: Record<string, string> = {
  GAME_NOT_FOUND: 'This game no longer exists. Start a new one.',
  ILLEGAL_MOVE: 'That move is not legal.',
  NOT_YOUR_TURN: 'Wait for the computer to move.',
  GAME_ALREADY_FINISHED: 'The game is over. Start a new one to keep playing.',
  CONCURRENT_UPDATE: 'The game changed in another window. Reload to continue.',
  ENGINE_UNAVAILABLE: 'The chess engine is not responding. Try again in a moment.',
  AI_SERVICE_UNAVAILABLE: 'The AI assistant is not responding. Try again in a moment.',
};

/** Converts an HTTP failure into a message fit for the player. */
export function userMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'Cannot reach the server. Check your connection.';
    }
    const body = error.error as Partial<ApiError> | null;
    if (body?.code && MESSAGES[body.code]) {
      return MESSAGES[body.code];
    }
    if (body?.code === 'VALIDATION_ERROR' && body.message) {
      return body.message;
    }
  }
  return 'Something went wrong. Please try again.';
}
