import { Component, effect, inject, input, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Analysis } from '../../core/models/analysis.model';
import { formatEvaluation } from '../../core/format';
import { AiService } from '../../core/services/ai.service';
import { userMessage } from '../../core/services/error-message';

interface ChatEntry {
  question: string;
  answer: string | null;
  error: string | null;
}

@Component({
  selector: 'app-ai-chat',
  imports: [FormsModule],
  templateUrl: './ai-chat.html',
  styleUrl: './ai-chat.css',
})
export class AiChat {
  private readonly ai = inject(AiService);

  readonly gameId = input.required<string>();
  /** Number of moves played; a new move makes the previous analysis stale. */
  readonly ply = input(0);

  protected readonly analysis = signal<Analysis | null>(null);
  protected readonly analyzing = signal(false);
  protected readonly analysisError = signal<string | null>(null);

  protected readonly entries = signal<ChatEntry[]>([]);
  protected readonly asking = signal(false);
  protected question = '';

  constructor() {
    effect(() => {
      this.ply();
      this.gameId();
      untracked(() => {
        this.analysis.set(null);
        this.analysisError.set(null);
      });
    });
    effect(() => {
      this.gameId();
      untracked(() => this.entries.set([]));
    });
  }

  protected formatEvaluation = formatEvaluation;

  protected analyze(): void {
    this.analyzing.set(true);
    this.analysisError.set(null);
    this.ai.analyze(this.gameId()).subscribe({
      next: (analysis) => {
        this.analysis.set(analysis);
        this.analyzing.set(false);
      },
      error: (error: unknown) => {
        this.analysisError.set(userMessage(error));
        this.analyzing.set(false);
      },
    });
  }

  protected ask(): void {
    const question = this.question.trim();
    if (!question || this.asking()) {
      return;
    }
    this.question = '';
    this.asking.set(true);
    const index = this.entries().length;
    this.entries.update((entries) => [...entries, { question, answer: null, error: null }]);
    const settle = (patch: Partial<ChatEntry>) =>
      this.entries.update((entries) => entries.map((e, i) => (i === index ? { ...e, ...patch } : e)));
    this.ai.ask(this.gameId(), question).subscribe({
      next: (response) => {
        settle({ answer: response.answer });
        this.asking.set(false);
      },
      error: (error: unknown) => {
        settle({ error: userMessage(error) });
        this.asking.set(false);
      },
    });
  }
}
