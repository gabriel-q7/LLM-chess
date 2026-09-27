import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { gameFixture } from '../../testing';
import { userMessage } from './error-message';
import { GameService } from './game.service';

describe('GameService', () => {
  let service: GameService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(GameService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('creates a game and keeps it as current state', () => {
    service.createGame('BLACK').subscribe();

    const request = http.expectOne('/api/games');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ playerColor: 'BLACK' });
    expect(service.busy()).toBe(true);
    request.flush(gameFixture({ playerColor: 'BLACK' }));

    expect(service.game()?.playerColor).toBe('BLACK');
    expect(service.busy()).toBe(false);
  });

  it('loads a game by id', () => {
    service.loadGame('game-1').subscribe();

    http.expectOne({ method: 'GET', url: '/api/games/game-1' }).flush(gameFixture());

    expect(service.game()?.id).toBe('game-1');
    expect(service.isPlayersTurn()).toBe(true);
  });

  it('submits moves for the current game', () => {
    service.createGame('WHITE').subscribe();
    http.expectOne('/api/games').flush(gameFixture());

    service.submitMove({ from: 'e2', to: 'e4' }).subscribe();

    const request = http.expectOne('/api/games/game-1/moves');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ from: 'e2', to: 'e4' });
    request.flush(gameFixture({ turn: 'WHITE', fen: 'after' }));
    expect(service.game()?.fen).toBe('after');
  });

  it('turns API errors into player-facing messages', () => {
    let message = '';
    service.createGame('WHITE').subscribe({ error: (e) => (message = userMessage(e)) });

    http.expectOne('/api/games').flush(
      { code: 'ENGINE_UNAVAILABLE', message: 'The chess engine is unavailable.' },
      { status: 503, statusText: 'Service Unavailable' },
    );

    expect(message).toBe('The chess engine is not responding. Try again in a moment.');
    expect(service.busy()).toBe(false);
  });
});
