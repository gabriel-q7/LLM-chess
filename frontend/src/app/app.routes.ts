import { Routes } from '@angular/router';
import { Game } from './features/game/game';

export const routes: Routes = [
  { path: '', component: Game, title: 'AI Chess' },
  { path: '**', redirectTo: '' },
];
