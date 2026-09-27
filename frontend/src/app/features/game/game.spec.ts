import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { gameFixture } from '../../testing';
import { Game } from './game';

describe('Game', () => {
  let fixture: ComponentFixture<Game>;
  let element: HTMLElement;
  let http: HttpTestingController;

  const click = (selector: string) => element.querySelector<HTMLElement>(selector)!.click();

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [Game],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(Game);
    element = fixture.nativeElement;
    http = TestBed.inject(HttpTestingController);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  it('creates a game and renders the board', async () => {
    click('[data-testid="new-white"]');
    http.expectOne('/api/games').flush(gameFixture());
    await fixture.whenStable();

    expect(element.querySelectorAll('[data-square]').length).toBe(64);
    expect(element.querySelector('[data-testid="status"]')?.textContent).toContain('Your move.');
  });

  it('creates the game at the chosen difficulty', async () => {
    click('[data-difficulty="HARD"]');
    await fixture.whenStable();
    expect(element.querySelector('[data-difficulty="HARD"]')?.getAttribute('aria-checked')).toBe('true');

    click('[data-testid="new-black"]');
    const request = http.expectOne('/api/games');
    expect(request.request.body).toEqual({ playerColor: 'BLACK', difficulty: 'HARD' });
    request.flush(gameFixture({ playerColor: 'BLACK', difficulty: 'HARD' }));
    await fixture.whenStable();

    expect(element.querySelector('[data-testid="difficulty"]')?.textContent).toContain('Hard');
  });

  it('submits the move and shows the updated game and history', async () => {
    click('[data-testid="new-white"]');
    http.expectOne('/api/games').flush(gameFixture());
    await fixture.whenStable();

    click('[data-square="e2"]');
    await fixture.whenStable();
    click('[data-square="e4"]');

    const request = http.expectOne('/api/games/game-1/moves');
    expect(request.request.body).toEqual({ from: 'e2', to: 'e4' });
    request.flush(
      gameFixture({
        fen: 'rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2',
        moves: [
          { ply: 1, moveNumber: 1, color: 'WHITE', from: 'e2', to: 'e4', promotion: null, san: 'e4', fen: '' },
          { ply: 2, moveNumber: 1, color: 'BLACK', from: 'e7', to: 'e5', promotion: null, san: 'e5', fen: '' },
        ],
      }),
    );
    await fixture.whenStable();

    expect(element.querySelector('[data-testid="history"]')?.textContent).toMatch(/1\.\s*e4\s*e5/);
    expect(element.querySelector('[data-square="e5"] [data-piece]')?.getAttribute('data-piece')).toBe('BLACK-p');
  });

  it('displays backend errors', async () => {
    click('[data-testid="new-white"]');
    http.expectOne('/api/games').flush(gameFixture());
    await fixture.whenStable();

    click('[data-square="e2"]');
    await fixture.whenStable();
    click('[data-square="e4"]');
    http
      .expectOne('/api/games/game-1/moves')
      .flush({ code: 'ILLEGAL_MOVE', message: 'x' }, { status: 422, statusText: 'Unprocessable Content' });
    await fixture.whenStable();

    expect(element.querySelector('[data-testid="error"]')?.textContent).toContain('That move is not legal.');
  });

  it('reports checkmate', async () => {
    click('[data-testid="new-white"]');
    http.expectOne('/api/games').flush(gameFixture({ status: 'CHECKMATE', winner: 'BLACK', check: true, legalMoves: [] }));
    await fixture.whenStable();

    expect(element.querySelector('[data-testid="status"]')?.textContent).toContain('Checkmate — Black wins.');
  });
});
