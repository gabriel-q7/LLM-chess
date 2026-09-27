import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Analysis, ChatRequest, ChatResponse } from '../models/analysis.model';

@Injectable({
  providedIn: 'root',
})
export class AiService {
  private readonly http = inject(HttpClient);

  /** Stockfish analysis of the current position, explained by the AI assistant. */
  analyze(gameId: string): Observable<Analysis> {
    return this.http.post<Analysis>(`/api/games/${gameId}/analysis`, null);
  }

  ask(gameId: string, question: string): Observable<ChatResponse> {
    const body: ChatRequest = { question };
    return this.http.post<ChatResponse>(`/api/games/${gameId}/chat`, body);
  }
}
