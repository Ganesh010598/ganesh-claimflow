import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ClaimApiService, Claim, ClaimActivity, errorMessage, displayStatus } from '../../../core/claim-api.service';

@Component({selector:'app-my-claims',standalone:true,imports:[CommonModule,FormsModule,RouterLink],template: `
<div class="eyebrow">CLAIMANT PORTAL / TRACKING</div><div class="page-heading"><div><h1>My claims</h1><p>View progress, claim decisions and requests for more information.</p></div><a class="btn btn-primary" routerLink="/new-claim">+ New claim</a></div>
<section class="card filter-card"><label class="filter-label">Customer ID <input [(ngModel)]="customerId" (keyup.enter)="load()" aria-label="Customer ID"/></label><button class="btn btn-secondary" (click)="load()">Find claims</button><span class="tiny-muted">Use demo-customer-001 or demo-customer-002</span></section>
@if(error){<div class="alert error" role="alert">{{error}}</div>}
@if(loading){<div class="card">Loading claims…</div>}
@if(!loading){<div class="two-column wide-left"><section class="card"><div class="section-title"><h2>Claim history</h2><span class="counter">{{claims.length}} found</span></div>
@if(!claims.length){<div class="empty">No claims found for this customer ID.</div>}
@for(claim of claims; track claim.id){<button type="button" class="claim-list-item" [class.selected]="selected?.id === claim.id" (click)="select(claim)"><div><strong>{{claim.claimNumber}}</strong><small>{{claim.claimType}} · {{claim.market}} · {{claim.createdAt | date:'mediumDate'}}</small></div><span class="status" [attr.data-status]="claim.status">{{status(claim.status)}}</span></button>}
</section><aside class="card details-card">@if(selected){<div class="section-title"><h2>{{selected.claimNumber}}</h2><span class="status" [attr.data-status]="selected.status">{{status(selected.status)}}</span></div><dl class="details"><div><dt>Claimant</dt><dd>{{selected.claimantName}}</dd></div><div><dt>Claimed amount</dt><dd>{{selected.currency}} {{selected.claimedAmount | number:'1.2-2'}}</dd></div><div><dt>Incident</dt><dd>{{selected.incidentDate}}</dd></div><div><dt>Description</dt><dd>{{selected.description}}</dd></div>@if(selected.approvedAmount!==null){<div><dt>Approved amount</dt><dd>{{selected.currency}} {{selected.approvedAmount | number:'1.2-2'}}</dd></div>}@if(selected.decisionReason){<div><dt>Decision reason</dt><dd>{{selected.decisionReason}}</dd></div>}</dl>
@if(selected.status==='NEEDS_INFORMATION'){<div class="request-box"><strong>Officer requested information</strong><p>{{selected.informationRequest}}</p><label>Your response<textarea rows="3" [(ngModel)]="additionalInfo" placeholder="Provide details..."></textarea></label><button class="btn btn-primary" [disabled]="!additionalInfo.trim() || saving" (click)="respond()">Send information</button></div>}
<h3 class="timeline-title">Activity timeline</h3><div class="timeline">@for(activity of activities;track activity.id){<div class="timeline-item"><strong>{{status(activity.action)}}</strong><small>{{activity.createdAt | date:'medium'}} · {{activity.actorId}}</small><p>{{activity.message}}</p></div>}@empty{<p class="tiny-muted">No activity yet.</p>}</div>
}@else{<div class="empty">Select a claim to view its details.</div>}</aside></div>}
`})
export class MyClaimsComponent implements OnInit {
 private api = inject(ClaimApiService);
 customerId = 'demo-customer-001'; claims:Claim[]=[]; selected:Claim|null=null;activities:ClaimActivity[]=[];
 additionalInfo='';error='';loading=false;saving=false; status=displayStatus;
 ngOnInit(){this.load();}
 load(){this.loading=true;this.error='';this.api.list(this.customerId.trim()).subscribe({next:claims=>{this.claims=claims;this.loading=false;this.selected=null;this.activities=[];if(claims.length)this.select(claims[0]);},error:e=>{this.error=errorMessage(e);this.loading=false;}});}
 select(claim:Claim){this.selected=claim;this.additionalInfo='';this.api.history(claim.id).subscribe({next:a=>this.activities=a,error:e=>this.error=errorMessage(e)});}
 respond(){if(!this.selected || !this.additionalInfo.trim())return;this.saving=true;this.error='';this.api.provideInfo(this.selected.id,this.additionalInfo.trim()).subscribe({next:updated=>{this.saving=false;this.claims=this.claims.map(c=>c.id===updated.id?updated:c);this.select(updated);},error:e=>{this.error=errorMessage(e);this.saving=false;}});}
}
