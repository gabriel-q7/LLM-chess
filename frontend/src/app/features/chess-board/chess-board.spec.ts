import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MoveRequest } from '../../core/models/move.model';
import { START_FEN } from '../../testing';
import { ChessBoard, parsePlacement } from './chess-board';

describe('ChessBoard', () => {
  let fixture: ComponentFixture<ChessBoard>;
  let element: HTMLElement;
  let emitted: MoveRequest[];

  const square = (name: string) => element.querySelector<HTMLButtonElement>(`[data-square="${name}"]`)!;

  async function render(inputs: Record<string, unknown>): Promise<void> {
    for (const [key, value] of Object.entries(inputs)) {
      fixture.componentRef.setInput(key, value);
    }
    await fixture.whenStable();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ChessBoard] }).compileComponents();
    fixture = TestBed.createComponent(ChessBoard);
    element = fixture.nativeElement;
    emitted = [];
    fixture.componentInstance.move.subscribe((m) => emitted.push(m));
    await render({
      fen: START_FEN,
      legalMoves: [
        { from: 'e2', to: 'e4', promotion: null },
        { from: 'e2', to: 'e3', promotion: null },
      ],
    });
  });

  it('renders 64 squares and the 32 pieces of the starting position', () => {
    expect(element.querySelectorAll('[data-square]').length).toBe(64);
    expect(element.querySelectorAll('[data-piece]').length).toBe(32);
    expect(square('e1').querySelector('[data-piece]')?.getAttribute('data-piece')).toBe('WHITE-k');
    expect(square('d8').querySelector('[data-piece]')?.getAttribute('data-piece')).toBe('BLACK-q');
  });

  it('puts a8 in the top-left corner for White and h1 for Black', async () => {
    expect(element.querySelector('[data-square]')?.getAttribute('data-square')).toBe('a8');
    await render({ orientation: 'BLACK' });
    expect(element.querySelector('[data-square]')?.getAttribute('data-square')).toBe('h1');
  });

  it('shows only the destinations supplied by the backend', async () => {
    square('e2').click();
    await fixture.whenStable();

    const targets = [...element.querySelectorAll('[data-target]')].map((t) => t.closest('[data-square]')?.getAttribute('data-square'));
    expect(targets.sort()).toEqual(['e3', 'e4']);
  });

  it('emits the move after selecting a piece and a destination', async () => {
    square('e2').click();
    await fixture.whenStable();
    square('e4').click();

    expect(emitted).toEqual([{ from: 'e2', to: 'e4' }]);
  });

  it('ignores destinations that are not legal', async () => {
    square('e2').click();
    await fixture.whenStable();
    square('e5').click();

    expect(emitted).toEqual([]);
  });

  it('does not select pieces without legal moves', async () => {
    square('d1').click();
    await fixture.whenStable();

    expect(element.querySelectorAll('[data-target]').length).toBe(0);
  });

  it('asks for the promotion piece', async () => {
    await render({
      fen: '8/P7/8/8/8/8/8/k6K w - - 0 1',
      legalMoves: ['q', 'r', 'b', 'n'].map((p) => ({ from: 'a7', to: 'a8', promotion: p })),
    });
    square('a7').click();
    await fixture.whenStable();
    square('a8').click();
    await fixture.whenStable();

    element.querySelector<HTMLButtonElement>('[data-promotion="n"]')!.click();

    expect(emitted).toEqual([{ from: 'a7', to: 'a8', promotion: 'n' }]);
  });

  it('ignores clicks when disabled', async () => {
    await render({ disabled: true });
    square('e2').click();
    await fixture.whenStable();

    expect(element.querySelectorAll('[data-target]').length).toBe(0);
  });

  it('parses FEN piece placement', () => {
    const pieces = parsePlacement('4k3/8/8/8/8/8/8/4K2R w K - 0 1');
    expect(pieces.size).toBe(3);
    expect(pieces.get('h1')).toEqual({ color: 'WHITE', type: 'r' });
  });
});
