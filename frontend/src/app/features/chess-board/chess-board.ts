import { Component, computed, input, output, signal } from '@angular/core';
import { Color } from '../../core/models/game.model';
import { LegalMove, MoveRequest, PromotionPiece } from '../../core/models/move.model';

interface Piece {
  color: Color;
  type: 'k' | 'q' | 'r' | 'b' | 'n' | 'p';
}

interface Square {
  name: string;
  light: boolean;
  piece: Piece | null;
  fileLabel: string | null;
  rankLabel: string | null;
}

const FILES = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'];

// Solid glyphs for both colours; U+FE0E asks for text rather than emoji rendering.
const GLYPHS: Record<Piece['type'], string> = {
  k: '♚︎',
  q: '♛︎',
  r: '♜︎',
  b: '♝︎',
  n: '♞︎',
  p: '♟︎',
};

const NAMES: Record<Piece['type'], string> = {
  k: 'king',
  q: 'queen',
  r: 'rook',
  b: 'bishop',
  n: 'knight',
  p: 'pawn',
};

/** Reads piece placement from a FEN for display. Rules are never evaluated here. */
export function parsePlacement(fen: string): Map<string, Piece> {
  const pieces = new Map<string, Piece>();
  const rows = fen.split(' ')[0].split('/');
  rows.forEach((row, index) => {
    const rank = 8 - index;
    let file = 0;
    for (const char of row) {
      const empty = Number(char);
      if (empty) {
        file += empty;
        continue;
      }
      const lower = char.toLowerCase() as Piece['type'];
      pieces.set(`${FILES[file]}${rank}`, { color: char === lower ? 'BLACK' : 'WHITE', type: lower });
      file++;
    }
  });
  return pieces;
}

@Component({
  selector: 'app-chess-board',
  imports: [],
  templateUrl: './chess-board.html',
  styleUrl: './chess-board.css',
})
export class ChessBoard {
  readonly fen = input.required<string>();
  readonly orientation = input<Color>('WHITE');
  /** Moves the player may make, as supplied by the backend. */
  readonly legalMoves = input<LegalMove[]>([]);
  readonly lastMove = input<{ from: string; to: string } | null>(null);
  readonly check = input(false);
  readonly disabled = input(false);

  readonly move = output<MoveRequest>();

  protected readonly selected = signal<string | null>(null);
  protected readonly pendingPromotion = signal<{ from: string; to: string } | null>(null);
  protected readonly promotionPieces: PromotionPiece[] = ['q', 'r', 'b', 'n'];

  protected readonly squares = computed<Square[]>(() => {
    const pieces = parsePlacement(this.fen());
    const white = this.orientation() === 'WHITE';
    const ranks = white ? [8, 7, 6, 5, 4, 3, 2, 1] : [1, 2, 3, 4, 5, 6, 7, 8];
    const files = white ? FILES : [...FILES].reverse();
    return ranks.flatMap((rank, row) =>
      files.map((file, col) => ({
        name: `${file}${rank}`,
        light: (FILES.indexOf(file) + rank) % 2 === 1,
        piece: pieces.get(`${file}${rank}`) ?? null,
        fileLabel: row === 7 ? file : null,
        rankLabel: col === 0 ? String(rank) : null,
      })),
    );
  });

  protected readonly targets = computed(() => {
    const from = this.selected();
    return new Set(this.legalMoves().filter((m) => m.from === from).map((m) => m.to));
  });

  protected readonly checkedKing = computed(() => {
    if (!this.check()) {
      return null;
    }
    const turn = this.fen().split(' ')[1] === 'w' ? 'WHITE' : 'BLACK';
    for (const [square, piece] of parsePlacement(this.fen())) {
      if (piece.type === 'k' && piece.color === turn) {
        return square;
      }
    }
    return null;
  });

  protected squareClass(square: Square): string {
    const last = this.lastMove();
    let background = square.light ? 'bg-amber-100' : 'bg-amber-700';
    if (this.checkedKing() === square.name) {
      background = 'bg-red-400';
    } else if (this.selected() === square.name) {
      background = 'bg-sky-300';
    } else if (last && (last.from === square.name || last.to === square.name)) {
      background = square.light ? 'bg-yellow-200' : 'bg-yellow-500';
    }
    return `${background} ${this.disabled() ? 'cursor-default' : 'cursor-pointer'}`;
  }

  protected glyph(piece: Piece): string {
    return GLYPHS[piece.type];
  }

  protected promotionGlyph(piece: PromotionPiece): string {
    return GLYPHS[piece];
  }

  protected label(square: Square): string {
    return square.piece
      ? `${square.name}, ${square.piece.color.toLowerCase()} ${NAMES[square.piece.type]}`
      : square.name;
  }

  protected onSquareClick(square: string): void {
    if (this.disabled() || this.pendingPromotion()) {
      return;
    }
    const from = this.selected();
    if (from && this.targets().has(square)) {
      const isPromotion = this.legalMoves().some((m) => m.from === from && m.to === square && m.promotion);
      this.selected.set(null);
      if (isPromotion) {
        this.pendingPromotion.set({ from, to: square });
      } else {
        this.move.emit({ from, to: square });
      }
      return;
    }
    const hasMoves = this.legalMoves().some((m) => m.from === square);
    this.selected.set(hasMoves && square !== from ? square : null);
  }

  protected choosePromotion(piece: PromotionPiece): void {
    const pending = this.pendingPromotion();
    this.pendingPromotion.set(null);
    if (pending) {
      this.move.emit({ ...pending, promotion: piece });
    }
  }

  protected cancelPromotion(): void {
    this.pendingPromotion.set(null);
  }
}
