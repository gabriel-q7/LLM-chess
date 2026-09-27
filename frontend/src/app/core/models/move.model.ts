import { Color } from './game.model';

export type PromotionPiece = 'q' | 'r' | 'b' | 'n';

export interface Move {
  ply: number;
  moveNumber: number;
  color: Color;
  from: string;
  to: string;
  promotion: PromotionPiece | null;
  san: string;
  fen: string;
}

export interface LegalMove {
  from: string;
  to: string;
  promotion: PromotionPiece | null;
}

export interface MoveRequest {
  from: string;
  to: string;
  promotion?: PromotionPiece;
}
