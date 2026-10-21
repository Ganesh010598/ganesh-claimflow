import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ClaimApiService, Dashboard, errorMessage } from '../../../core/claim-api.service';

@Component({selector:'app-dashboard',standalone:true,imports:[CommonModule],template: `
<div class="page-heading"><div><h1>Dashboard</h1><p>Claim activity and workload overview.</p></div><button class="btn btn-secondary" (click)="load()">↻ Refresh</button></div>
@if(error){<div class="alert error" role="alert">{{error}}</div>}
@if(loading){<section class="card">Loading dashboard…</section>}
@if(data){<div class="metric-grid"><div class="metric card"><div class="metric-icon">▦</div><span>Total claims</span><strong>{{data.totalClaims}}</strong><small>All statuses</small></div><div class="metric card"><div class="metric-icon">○</div><span>Open claims</span><strong>{{data.openClaims}}</strong><small>Active claims</small></div><div class="metric card"><div class="metric-icon">◷</div><span>Awaiting review</span><strong>{{data.awaitingReview}}</strong><small>Review pending</small></div><div class="metric card"><div class="metric-icon">✉</div><span>Pending customer</span><strong>{{data.awaitingCustomer}}</strong><small>Waiting for a reply</small></div></div>
<div class="two-column"><section class="card"><div class="section-title"><h2>Outstanding exposure</h2></div><p class="tiny-muted">By market and currency</p>
<div class="exposure-list">@for(x of data.exposureByMarket;track x.market){<div class="exposure-row"><div><strong>{{x.market}}</strong><small>{{x.currency}}</small></div><strong>{{x.amount | number:'1.2-2'}} {{x.currency}}</strong></div>}@empty{<div class="empty">No unsettled exposure.</div>}</div></section>
<section class="card"><div class="section-title"><h2>Officer workload</h2></div><p class="tiny-muted">Open claims by officer</p>
@for(item of data.workloadByOfficer | keyvalue;track item.key){<div class="exposure-row"><strong>{{item.key}}</strong><span class="counter">{{item.value}} open</span></div>}@empty{<div class="empty">No assigned open claims.</div>}</section></div>
<div class="note-panel" style="margin-top:20px">Exposure figures are estimates, shown separately by currency. They are not actuarial reserves.</div>
}
`})
export class DashboardComponent implements OnInit {
 private api=inject(ClaimApiService);data:Dashboard|null=null;error='';loading=false;
 ngOnInit(){this.load();}
 load(){this.loading=true;this.error='';this.api.dashboard().subscribe({next:d=>{this.data=d;this.loading=false;},error:e=>{this.error=errorMessage(e);this.loading=false;}});}
}
