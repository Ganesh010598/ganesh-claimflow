import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ClaimApiService, Claim, ClaimStatus, ClaimActivity, errorMessage, displayStatus } from '../../../core/claim-api.service';

@Component({selector:'app-my-claims',standalone:true,imports:[CommonModule,FormsModule,RouterLink],template: `
<div class="page-heading"><div><h1>My claims</h1><p>Track your claims and respond to requests.</p></div><a class="btn btn-primary" routerLink="/new-claim">+ New claim</a></div>
<section class="card filter-card" aria-label="Claim filters">
  <label class="filter-label">Customer ID<input [(ngModel)]="customerId" (keyup.enter)="applyFilters()" placeholder="e.g. demo-customer-001" aria-label="Customer ID"/></label>
  <label class="filter-label">Status<select [(ngModel)]="statusFilter" aria-label="Filter claim status"><option value="">All statuses</option><option value="SUBMITTED">Submitted</option><option value="IN_REVIEW">In review</option><option value="NEEDS_INFORMATION">Needs information</option><option value="APPROVED">Approved</option><option value="REJECTED">Rejected</option><option value="SETTLED">Settled</option></select></label>
  <button class="btn btn-secondary filter-submit" type="button" [disabled]="loading" (click)="applyFilters()">Search claims</button>
</section>
@if(error){<div class="alert error" role="alert">{{error}}</div>}
@if(loading){<div class="card">Loading claims…</div>}
@if(!loading){<div class="two-column wide-left"><section class="card"><div class="section-title"><h2>Claims</h2><span class="counter">{{totalElements}} total</span></div>
@if(!claims.length){<div class="empty">No claims found for this customer ID.</div>}
@for(claim of claims; track claim.id){<button type="button" class="claim-list-item" [class.selected]="selected?.id === claim.id" (click)="select(claim)"><div><strong>{{claim.claimNumber}}</strong><small>{{claim.claimType}} · {{claim.market}} · {{claim.createdAt | date:'mediumDate'}}</small></div><span class="status" [attr.data-status]="claim.status">{{status(claim.status)}}</span></button>}
<div class="pagination"><button class="btn btn-secondary" [disabled]="loading || page===0" (click)="previousPage()">← Previous</button><span class="page-count">{{page + 1}} / {{totalPages || 1}}</span><button class="btn btn-secondary" [disabled]="loading || page + 1 >= totalPages" (click)="nextPage()">Next →</button></div></section><aside class="card details-card">@if(selected){<div class="section-title"><h2>{{selected.claimNumber}}</h2><span class="status" [attr.data-status]="selected.status">{{status(selected.status)}}</span></div><dl class="details"><div><dt>Claimant</dt><dd>{{selected.claimantName}}</dd></div><div><dt>Claimed amount</dt><dd>{{selected.currency}} {{selected.claimedAmount | number:'1.2-2'}}</dd></div><div><dt>Incident</dt><dd>{{selected.incidentDate}}</dd></div><div><dt>Description</dt><dd>{{selected.description}}</dd></div>@if(selected.approvedAmount!==null){<div><dt>Approved amount</dt><dd>{{selected.currency}} {{selected.approvedAmount | number:'1.2-2'}}</dd></div>}@if(selected.decisionReason){<div><dt>Decision reason</dt><dd>{{selected.decisionReason}}</dd></div>}</dl>
@if(selected.status==='NEEDS_INFORMATION'){<div class="request-box"><strong>Officer requested information</strong><p>{{selected.informationRequest}}</p><label>Your response<textarea rows="3" [(ngModel)]="additionalInfo" placeholder="Provide details..."></textarea></label><button class="btn btn-primary" [disabled]="!additionalInfo.trim() || saving" (click)="respond()">Send information</button></div>}
<h3 class="timeline-title">Activity</h3><div class="timeline">@for(activity of activities;track activity.id){<div class="timeline-item"><strong>{{status(activity.action)}}</strong><small>{{activity.createdAt | date:'medium'}} · {{activity.actorId}}</small><p>{{activity.message}}</p></div>}@empty{<p class="tiny-muted">No activity yet.</p>}</div>
}@else{<div class="empty">Select a claim to see the details.</div>}</aside></div>}
`})
export class MyClaimsComponent implements OnInit {
 private api = inject(ClaimApiService);
 customerId = 'demo-customer-001'; claims:Claim[]=[]; selected:Claim|null=null;activities:ClaimActivity[]=[];
 additionalInfo='';error='';loading=false;saving=false; status=displayStatus;
 page=0;pageSize=10;totalPages=0;totalElements=0;statusFilter:ClaimStatus|''='';
 ngOnInit(){this.load();}
 applyFilters(){this.page=0;this.load();}
 previousPage(){if(this.page>0){this.page--;this.load();}}
 nextPage(){if(this.page+1<this.totalPages){this.page++;this.load();}}
 load(){
   this.loading=true;this.error='';
   this.api.listPage(this.page,this.pageSize,this.customerId.trim(),this.statusFilter||undefined)
     .subscribe({next:result=>{
       this.claims=result.content;this.page=result.page;this.totalPages=result.totalPages;
       this.totalElements=result.totalElements;this.loading=false;
       this.selected=null;this.activities=[];
       if(this.claims.length)this.select(this.claims[0]);
     },error:e=>{this.error=errorMessage(e);this.loading=false;}});
 }
 select(claim:Claim){this.selected=claim;this.additionalInfo='';this.api.history(claim.id).subscribe({next:a=>this.activities=a,error:e=>this.error=errorMessage(e)});}
 respond(){if(!this.selected || !this.additionalInfo.trim())return;this.saving=true;this.error='';this.api.provideInfo(this.selected.id,this.additionalInfo.trim()).subscribe({next:updated=>{this.saving=false;this.claims=this.claims.map(c=>c.id===updated.id?updated:c);this.select(updated);},error:e=>{this.error=errorMessage(e);this.saving=false;}});}
}
