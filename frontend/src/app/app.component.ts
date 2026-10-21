import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <header class="topbar">
      <div class="layout header-content">
        <a class="brand" routerLink="/new-claim" aria-label="Ganesh's ClaimFlow home">
          <span class="brand-icon" aria-hidden="true">G</span>
          <span class="brand-name">Ganesh's <strong>ClaimFlow</strong></span>
        </a>
        <span class="demo-pill">Demo workspace</span>
      </div>
    </header>
    <div class="subnav">
      <nav class="layout nav-content" aria-label="Main navigation">
        <span class="nav-caption">CLAIMANT</span>
        <a routerLink="/new-claim" routerLinkActive="active">New claim</a>
        <a routerLink="/my-claims" routerLinkActive="active">My claims</a>
        <span class="nav-divider" aria-hidden="true"></span>
        <span class="nav-caption">OFFICER</span>
        <a routerLink="/officer/queue" routerLinkActive="active">Claims queue</a>
        <a routerLink="/officer/dashboard" routerLinkActive="active">Dashboard</a>
      </nav>
    </div>
    <main class="layout main-content"><router-outlet /></main>
    <footer class="layout footer"><span>© 2026 Ganesh's ClaimFlow</span><span>Assessment prototype · Demo data only</span></footer>
  `
})
export class AppComponent {}
