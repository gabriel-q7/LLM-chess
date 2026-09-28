import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { DecimalPipe, KeyValuePipe } from '@angular/common';
import { ComputerMoveContext, LayaDecision } from '../../core/models/ai-context.model';
import { DIFFICULTY_LABELS, formatEvaluation } from '../../core/format';
import { AiService } from '../../core/services/ai.service';
import { GameService } from '../../core/services/game.service';
import { userMessage } from '../../core/services/error-message';

/**
 * Debug view of what the AI side received: Stockfish's input and output for the computer's last
 * move, and the exact context and decisions of the assistant's last reply.
 */
@Component({
  selector: 'app-ai-context',
  imports: [DecimalPipe, KeyValuePipe],
  templateUrl: './ai-context.html',
  styleUrl: './ai-context.css',
})
export class AiContext {
  private readonly games = inject(GameService);
  private readonly ai = inject(AiService);

  readonly gameId = input.required<string>();
  /** Number of moves played; a change means a new computer move to fetch. */
  readonly ply = input(0);

  protected readonly engine = signal<ComputerMoveContext | null>(null);
  protected readonly engineError = signal<string | null>(null);
  protected readonly trace = computed(() => {
    const last = this.ai.lastTrace();
    return last && last.gameId === this.gameId() ? last : null;
  });

  protected readonly difficultyLabels = DIFFICULTY_LABELS;
  protected readonly formatEvaluation = formatEvaluation;
  protected readonly keepOrder = () => 0;

  constructor() {
    effect((onCleanup) => {
      const gameId = this.gameId();
      this.ply();
      this.engineError.set(null);
      const subscription = this.games.engineContext(gameId).subscribe({
        next: (context) => this.engine.set(context),
        error: (error: unknown) => {
          this.engine.set(null);
          this.engineError.set(userMessage(error));
        },
      });
      // A newer move supersedes a request still in flight.
      onCleanup(() => subscription.unsubscribe());
    });
  }

  protected percent(value: number): string {
    return `${Math.round(value * 100)}%`;
  }

  protected sortedProbabilities(decision: LayaDecision): [string, number][] {
    return Object.entries(decision.probabilities).sort((a, b) => b[1] - a[1]);
  }
}
