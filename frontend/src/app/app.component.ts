import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <header class="topbar">
      <div class="layout header-content">
        <a class="brand" routerLink="/new-claim"><span class="brand-icon">✦</span> Claims<span>Flow</span></a>
        <span class="demo-pill">DEMO APPLICATION · NO AUTHENTICATION</span>
      </div>
    </header>
    <div class="subnav"><nav class="layout nav-content" aria-label="Application navigation">
      <span class="nav-caption">CUSTOMER</span>
      <a routerLink="/new-claim" routerLinkActive="active">New claim</a>
      <a routerLink="/my-claims" routerLinkActive="active">My claims</a>
      <span class="nav-divider"></span><span class="nav-caption">CLAIMS TEAM</span>
      <a routerLink="/officer/queue" routerLinkActive="active">Claims queue</a>
      <a routerLink="/officer/dashboard" routerLinkActive="active">Dashboard</a>
    </nav></div>
    <main class="layout main-content"><router-outlet /></main>
    <footer class="layout footer">Engineering assessment prototype · Demo data only · No real insurance processing</footer>
  `
})
export class AppComponent {}
