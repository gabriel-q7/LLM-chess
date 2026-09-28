import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AiTrace, ComputerMoveContext } from '../../core/models/ai-context.model';
import { AiService } from '../../core/services/ai.service';
import { AiContext } from './ai-context';

const ENGINE: ComputerMoveContext = {
  ply: 2,
  san: 'e5',
  fen: 'rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1',
  difficulty: 'HARD',
  engine: {
    move: 'e7e5',
    strength: { skillLevel: 20, depth: 14 },
    depth: 14,
    evaluation: 0.31,
    mateIn: null,
    principalVariation: ['e7e5', 'g1f3'],
    uciCommands: ['uci', 'setoption name Skill Level value 20', 'go depth 14'],
    finalInfo: 'info depth 14 score cp -31 pv e7e5 g1f3',
    bestMoveLine: 'bestmove e7e5 ponder g1f3',
  },
  principalVariationSan: ['e5', 'Nf3'],
};

const TRACE: AiTrace = {
  disclosure: { bestMove: false, pieceHint: false, continuation: false, numericEvaluation: false, playerMate: 'HIDDEN' },
  hiddenFacts: ['best move', 'FEN'],
  exchanges: [
    {
      model: 'english',
      state: 'A chess player asks: Who is winning?',
      questions: { intent: { type: 'choice', instructions: 'What is the player asking about?', criteria: { evaluation: 'who is winning' } } },
      decisions: { intent: { choice: 'evaluation', probability: 0.78, used: true, probabilities: { evaluation: 0.78, other: 0.22 } } },
    },
  ],
};

describe('AiContext', () => {
  let fixture: ComponentFixture<AiContext>;
  let element: HTMLElement;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AiContext],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(AiContext);
    fixture.componentRef.setInput('gameId', 'game-1');
    element = fixture.nativeElement;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('shows what Stockfish received and returned for the computer move', async () => {
    http.expectOne('/api/games/game-1/engine-context').flush(ENGINE);
    await fixture.whenStable();

    const text = element.querySelector('[data-testid="engine-context"]')!.textContent!;
    expect(text).toContain('e5');
    expect(text).toContain('skill 20');
    expect(text).toContain('+0.31');
    expect(text).toContain('e5 Nf3');
    expect(element.textContent).toContain(ENGINE.fen);
    expect(element.querySelector('[data-testid="uci"]')!.textContent).toContain('> go depth 14');
  });

  it('says when the computer has not moved yet', async () => {
    http.expectOne('/api/games/game-1/engine-context').flush(null, { status: 204, statusText: 'No Content' });
    await fixture.whenStable();

    expect(element.textContent).toContain('The computer has not moved yet.');
  });

  it('refetches after a new move', async () => {
    http.expectOne('/api/games/game-1/engine-context').flush(null, { status: 204, statusText: 'No Content' });
    fixture.componentRef.setInput('ply', 2);
    fixture.detectChanges();

    http.expectOne('/api/games/game-1/engine-context').flush(ENGINE);
  });

  it('shows the state, questions and decisions of the last Laya exchange', async () => {
    http.expectOne('/api/games/game-1/engine-context').flush(null, { status: 204, statusText: 'No Content' });
    TestBed.inject(AiService).ask('game-1', 'Who is winning?').subscribe();
    http.expectOne('/api/games/game-1/chat').flush({ answer: 'Equal.', trace: TRACE });
    await fixture.whenStable();

    const laya = element.querySelector('[data-testid="laya-context"]')!;
    expect(laya.textContent).toContain('Who is winning?');
    expect(laya.textContent).toContain('best move');
    expect(laya.querySelector('[data-testid="laya-state"]')!.textContent).toBe('A chess player asks: Who is winning?');
    expect(laya.textContent).toContain('evaluation');
    expect(laya.querySelector('[data-used]')!.textContent).toContain('used');
  });

  it('ignores traces from other games', async () => {
    http.expectOne('/api/games/game-1/engine-context').flush(null, { status: 204, statusText: 'No Content' });
    TestBed.inject(AiService).ask('other-game', 'Hi').subscribe();
    http.expectOne('/api/games/other-game/chat').flush({ answer: 'x', trace: TRACE });
    await fixture.whenStable();

    expect(element.querySelector('[data-testid="laya-context"]')).toBeNull();
  });
});
