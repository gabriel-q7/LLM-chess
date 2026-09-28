import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { AiTrace } from '../models/ai-context.model';
import { Analysis, ChatRequest, ChatResponse } from '../models/analysis.model';

export interface TraceRecord {
  gameId: string;
  source: 'analysis' | 'chat';
  /** The question, for chat traces. */
  question: string | null;
  trace: AiTrace;
}

@Injectable({
  providedIn: 'root',
})
export class AiService {
  private readonly http = inject(HttpClient);
  private readonly last = signal<TraceRecord | null>(null);

  /** What the assistant received for its most recent reply. */
  readonly lastTrace = this.last.asReadonly();

  /** Stockfish analysis of the current position, explained by the AI assistant. */
  analyze(gameId: string): Observable<Analysis> {
    return this.http
      .post<Analysis>(`/api/games/${gameId}/analysis`, null)
      .pipe(tap((a) => this.record(gameId, 'analysis', null, a.trace)));
  }

  ask(gameId: string, question: string): Observable<ChatResponse> {
    const body: ChatRequest = { question };
    return this.http
      .post<ChatResponse>(`/api/games/${gameId}/chat`, body)
      .pipe(tap((r) => this.record(gameId, 'chat', question, r.trace)));
  }

  private record(gameId: string, source: TraceRecord['source'], question: string | null, trace: AiTrace | null): void {
    if (trace) {
      this.last.set({ gameId, source, question, trace });
    }
  }
}
