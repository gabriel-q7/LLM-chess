import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AiService } from './ai.service';

describe('AiService', () => {
  let service: AiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('requests analysis', () => {
    let explanation = '';
    service.analyze('game-1').subscribe((a) => (explanation = a.explanation));

    const request = http.expectOne('/api/games/game-1/analysis');
    expect(request.request.method).toBe('POST');
    request.flush({ bestMove: 'Nf3', evaluation: 0.3, depth: 18, mateIn: null, explanation: 'Equal.' });

    expect(explanation).toBe('Equal.');
  });

  it('sends questions', () => {
    service.ask('game-1', 'Why?').subscribe();

    const request = http.expectOne('/api/games/game-1/chat');
    expect(request.request.body).toEqual({ question: 'Why?' });
    request.flush({ answer: 'Because.', bestMove: null, evaluation: 0, depth: 1, mateIn: null });
  });
});
