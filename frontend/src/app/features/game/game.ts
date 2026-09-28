import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { Color, Difficulty, Game as GameModel } from '../../core/models/game.model';
import { DIFFICULTY_LABELS } from '../../core/format';
import { PIECE_GLYPHS, describeMove, movedPiece } from '../../core/pieces';
import { Move, MoveRequest } from '../../core/models/move.model';
import { GameService } from '../../core/services/game.service';
import { userMessage } from '../../core/services/error-message';
import { AiChat } from '../ai-chat/ai-chat';
import { AiContext } from '../ai-context/ai-context';
import { ChessBoard } from '../chess-board/chess-board';

interface MovePair {
  number: number;
  white: Move | null;
  black: Move | null;
}

@Component({
  selector: 'app-game',
  imports: [ChessBoard, AiChat, AiContext],
  templateUrl: './game.html',
  styleUrl: './game.css',
})
export class Game implements OnInit {
  protected readonly games = inject(GameService);

  protected readonly game = this.games.game;
  protected readonly error = signal<string | null>(null);
  protected readonly difficulty = signal<Difficulty>('MEDIUM');
  protected readonly difficulties: Difficulty[] = ['EASY', 'MEDIUM', 'HARD'];
  protected readonly difficultyLabels = DIFFICULTY_LABELS;
  /** Debug panel left of the board with what Stockfish and Laya received. */
  protected readonly showContext = signal(readShowContext());

  protected glyph(move: Move): string {
    return PIECE_GLYPHS[movedPiece(move.san)];
  }

  protected glyphType(move: Move): string {
    return movedPiece(move.san);
  }

  protected readonly describeMove = describeMove;

  protected readonly lastMove = computed(() => this.game()?.moves.at(-1) ?? null);

  protected readonly status = computed(() => describeStatus(this.game(), this.games.busy()));

  protected readonly movePairs = computed<MovePair[]>(() => {
    const pairs = new Map<number, MovePair>();
    for (const move of this.game()?.moves ?? []) {
      const pair = pairs.get(move.moveNumber) ?? { number: move.moveNumber, white: null, black: null };
      if (move.color === 'WHITE') {
        pair.white = move;
      } else {
        pair.black = move;
      }
      pairs.set(move.moveNumber, pair);
    }
    return [...pairs.values()];
  });

  ngOnInit(): void {
    const saved = this.games.savedGameId();
    if (saved) {
      this.run(this.games.loadGame(saved), () => this.games.forgetSavedGame());
    }
  }

  protected toggleContext(): void {
    const show = !this.showContext();
    this.showContext.set(show);
    try {
      localStorage.setItem(SHOW_CONTEXT_KEY, String(show));
    } catch {
      // Remembering the toggle is a convenience; ignore storage failures.
    }
  }

  protected newGame(color: Color): void {
    this.run(this.games.createGame(color, this.difficulty()));
  }

  protected onMove(move: MoveRequest): void {
    this.run(this.games.submitMove(move));
  }

  private run(request: Observable<GameModel>, onError?: () => void): void {
    this.error.set(null);
    request.subscribe({
      error: (error: unknown) => {
        this.error.set(userMessage(error));
        onError?.();
      },
    });
  }
}

const SHOW_CONTEXT_KEY = 'chess-ai.showContext';

function readShowContext(): boolean {
  try {
    return localStorage.getItem(SHOW_CONTEXT_KEY) === 'true';
  } catch {
    return false;
  }
}

function describeStatus(game: GameModel | null, busy: boolean): string {
  if (!game) {
    return busy ? 'Starting…' : 'Start a new game to play against the computer.';
  }
  const side = (color: Color) => (color === 'WHITE' ? 'White' : 'Black');
  switch (game.status) {
    case 'CHECKMATE':
      return `Checkmate — ${side(game.winner ?? game.playerColor)} wins${game.winner === game.playerColor ? '. Well played!' : '.'}`;
    case 'STALEMATE':
      return 'Draw by stalemate.';
    case 'DRAW':
      return `Draw${drawReason(game.drawReason)}.`;
  }
  if (busy || game.turn !== game.playerColor) {
    return 'Computer is thinking…';
  }
  return game.check ? 'Your move — you are in check!' : 'Your move.';
}

function drawReason(reason: GameModel['drawReason']): string {
  switch (reason) {
    case 'REPETITION':
      return ' by threefold repetition';
    case 'INSUFFICIENT_MATERIAL':
      return ' by insufficient material';
    case 'FIFTY_MOVE_RULE':
      return ' by the fifty-move rule';
    default:
      return '';
  }
}
