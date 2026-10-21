import { Routes } from '@angular/router';
export const routes: Routes = [
  { path: '', redirectTo: 'new-claim', pathMatch: 'full' },
  { path: 'new-claim', loadComponent: () => import('./features/claimant/new-claim/new-claim.component').then(m => m.NewClaimComponent) },
  { path: 'my-claims', loadComponent: () => import('./features/claimant/my-claims/my-claims.component').then(m => m.MyClaimsComponent) },
  { path: 'officer/queue', loadComponent: () => import('./features/officer/claims-queue/claims-queue.component').then(m => m.ClaimsQueueComponent) },
  { path: 'officer/dashboard', loadComponent: () => import('./features/officer/dashboard/dashboard.component').then(m => m.DashboardComponent) },
  { path: '**', redirectTo: 'new-claim' }
];
