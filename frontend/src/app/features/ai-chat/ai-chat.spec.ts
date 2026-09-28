import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AiChat } from './ai-chat';

describe('AiChat', () => {
  let fixture: ComponentFixture<AiChat>;
  let element: HTMLElement;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AiChat],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(AiChat);
    fixture.componentRef.setInput('gameId', 'game-1');
    element = fixture.nativeElement;
    http = TestBed.inject(HttpTestingController);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  it('displays the engine analysis and AI explanation', async () => {
    element.querySelector<HTMLButtonElement>('[data-testid="analyze"]')!.click();
    http
      .expectOne('/api/games/game-1/analysis')
      .flush({ bestMove: 'Bb5', evaluation: 0.42, depth: 18, mateIn: null, explanation: 'White is slightly better.' });
    await fixture.whenStable();

    const analysis = element.querySelector('[data-testid="analysis"]')!.textContent!;
    expect(analysis).toContain('Bb5');
    expect(analysis).toContain('+0.42');
    expect(analysis).toContain('18');
    expect(analysis).toContain('White is slightly better.');
  });

  it('hides the engine numbers the difficulty does not disclose', async () => {
    element.querySelector<HTMLButtonElement>('[data-testid="analyze"]')!.click();
    http
      .expectOne('/api/games/game-1/analysis')
      .flush({ bestMove: null, evaluation: null, depth: 18, mateIn: null, explanation: 'Black is slightly better.' });
    await fixture.whenStable();

    expect(element.querySelector('[data-testid="best-move"]')).toBeNull();
    expect(element.querySelector('[data-testid="evaluation"]')).toBeNull();
    expect(element.querySelector('[data-testid="analysis"]')!.textContent).toContain('Black is slightly better.');
  });

  it('shows a friendly message when the AI is unavailable', async () => {
    element.querySelector<HTMLButtonElement>('[data-testid="analyze"]')!.click();
    http
      .expectOne('/api/games/game-1/analysis')
      .flush({ code: 'AI_SERVICE_UNAVAILABLE', message: 'x' }, { status: 503, statusText: 'Service Unavailable' });
    await fixture.whenStable();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain('The AI assistant is not responding');
  });

  it('sends a question and shows the answer', async () => {
    const input = element.querySelector<HTMLInputElement>('#question')!;
    input.value = 'Who is winning?';
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    element.querySelector('form')!.dispatchEvent(new Event('submit'));

    const request = http.expectOne('/api/games/game-1/chat');
    expect(request.request.body).toEqual({ question: 'Who is winning?' });
    request.flush({ answer: 'The position is roughly equal.', trace: null });
    await fixture.whenStable();

    const log = element.querySelector('[data-testid="chat-log"]')!.textContent!;
    expect(log).toContain('Who is winning?');
    expect(log).toContain('The position is roughly equal.');
  });

  it('clears a stale analysis after a new move', async () => {
    element.querySelector<HTMLButtonElement>('[data-testid="analyze"]')!.click();
    http.expectOne('/api/games/game-1/analysis').flush({ bestMove: 'e4', evaluation: 0, depth: 1, mateIn: null, explanation: 'x' });
    await fixture.whenStable();

    fixture.componentRef.setInput('ply', 2);
    await fixture.whenStable();

    expect(element.querySelector('[data-testid="analysis"]')).toBeNull();
  });
});
