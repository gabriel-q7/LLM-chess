import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, finalize, tap } from 'rxjs';
import { Color, CreateGameRequest, Difficulty, Game } from '../models/game.model';
import { ComputerMoveContext } from '../models/ai-context.model';
import { MoveRequest } from '../models/move.model';

const STORAGE_KEY = 'chess-ai.gameId';

@Injectable({
  providedIn: 'root',
})
export class GameService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/games';

  private readonly state = signal<Game | null>(null);
  private readonly pending = signal(false);

  /** The game being played, as last reported by the backend. */
  readonly game = this.state.asReadonly();
  /** True while a request that changes the game is in flight. */
  readonly busy = this.pending.asReadonly();
  readonly isPlayersTurn = computed(() => {
    const game = this.state();
    return !!game && game.status === 'PLAYING' && game.turn === game.playerColor;
  });

  createGame(playerColor: Color, difficulty: Difficulty): Observable<Game> {
    const body: CreateGameRequest = { playerColor, difficulty };
    return this.track(this.http.post<Game>(this.baseUrl, body));
  }

  loadGame(id: string): Observable<Game> {
    return this.track(this.http.get<Game>(`${this.baseUrl}/${id}`));
  }

  submitMove(move: MoveRequest): Observable<Game> {
    const game = this.state();
    if (!game) {
      throw new Error('No game in progress');
    }
    return this.track(this.http.post<Game>(`${this.baseUrl}/${game.id}/moves`, move));
  }

  /** What Stockfish received and returned for the computer's latest move; null before its first move. */
  engineContext(gameId: string): Observable<ComputerMoveContext | null> {
    return this.http.get<ComputerMoveContext | null>(`${this.baseUrl}/${gameId}/engine-context`);
  }

  /** Id of the game played last in this browser, if any. */
  savedGameId(): string | null {
    try {
      return localStorage.getItem(STORAGE_KEY);
    } catch {
      return null;
    }
  }

  forgetSavedGame(): void {
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      // Storage unavailable: nothing to forget.
    }
  }

  private track(request: Observable<Game>): Observable<Game> {
    this.pending.set(true);
    return request.pipe(
      tap((game) => {
        this.state.set(game);
        try {
          localStorage.setItem(STORAGE_KEY, game.id);
        } catch {
          // Resuming after reload is a convenience; ignore storage failures.
        }
      }),
      finalize(() => this.pending.set(false)),
    );
  }
}
