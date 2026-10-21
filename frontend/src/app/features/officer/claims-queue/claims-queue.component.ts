import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { ClaimApiService, Claim, ClaimActivity, errorMessage, displayStatus } from '../../../core/claim-api.service';

@Component({selector:'app-claims-queue',standalone:true,imports:[CommonModule,FormsModule],template: `
<div class="eyebrow">OPERATIONS / CLAIMS DESK</div><div class="page-heading"><div><h1>Claims queue</h1><p>Review incoming claims, request supporting information, and record decisions.</p></div><button class="btn btn-secondary" (click)="load()">↻ Refresh</button></div>
<section class="card filter-card"><label class="filter-label">Acting officer <input [(ngModel)]="officerId" aria-label="Acting officer" /></label><span class="tiny-muted">Demo: officer-001 or officer-002. This is not authentication.</span></section>
@if(error){<div class="alert error" role="alert">{{error}}</div>}@if(message){<div class="alert success" role="status">{{message}}</div>}
<div class="two-column wide-left"><section class="card"><div class="section-title"><h2>All claims</h2><span class="counter">{{claims.length}} total</span></div>
@if(loading){<p>Loading claims…</p>}
@for(c of claims;track c.id){<button class="claim-list-item" [class.selected]="selected?.id===c.id" (click)="select(c)"><div><strong>{{c.claimNumber}}</strong><small>{{c.claimantName}} · {{c.market}} · {{c.currency}} {{c.claimedAmount | number:'1.2-2'}}</small><small>Assigned: {{c.assignedOfficerId||'Unassigned'}}</small></div><span class="status" [attr.data-status]="c.status">{{status(c.status)}}</span></button>}@empty{<div class="empty">No claims yet. Submit one from New claim.</div>}
</section><aside class="card details-card">@if(selected){<div class="section-title"><h2>Process claim</h2><span class="status" [attr.data-status]="selected.status">{{status(selected.status)}}</span></div>
<div class="detail-highlight"><strong>{{selected.claimNumber}}</strong><p>{{selected.description}}</p><small>{{selected.claimantName}} · {{selected.claimType}} · {{selected.incidentDate}}</small></div>
<dl class="details"><div><dt>Claimed</dt><dd>{{selected.currency}} {{selected.claimedAmount | number:'1.2-2'}}</dd></div><div><dt>Reserve</dt><dd>@if(selected.reserveAmount === null) { Not assessed } @else { {{selected.currency}} {{selected.reserveAmount | number:'1.2-2'}} }</dd></div><div><dt>Assigned officer</dt><dd>{{selected.assignedOfficerId||'Unassigned'}}</dd></div></dl>
@if(selected.status==='SUBMITTED'){
  <div class="action-panel"><h3>Take ownership</h3><p class="tiny-muted">Assignment moves the claim into review.</p><button class="btn btn-primary" [disabled]="saving || !officerId.trim()" (click)="execute(api.assign(selected.id,officerId),'Claim assigned')">Assign to me →</button></div>
}
@if(selected.status==='IN_REVIEW'){
 <div class="action-panel"><h3>Update reserve estimate</h3><div class="inline-fields"><input aria-label="Reserve amount" type="number" min="0.01" step="0.01" [(ngModel)]="reserveAmount"/><button class="btn btn-secondary" [disabled]="saving || reserveAmount<=0" (click)="execute(api.reserve(selected.id,officerId,reserveAmount),'Reserve updated')">Save reserve</button></div></div>
 <div class="action-panel"><h3>Request more information</h3><textarea rows="2" [(ngModel)]="infoMessage" placeholder="Describe what evidence is needed..."></textarea><button class="btn btn-secondary" [disabled]="saving || !infoMessage.trim()" (click)="execute(api.requestInfo(selected.id,officerId,infoMessage),'Information requested')">Request information</button></div>
 <div class="action-panel"><h3>Record decision</h3><label>Decision reason <textarea rows="2" [(ngModel)]="reason" placeholder="Explain assessment outcome..."></textarea></label><label>Approved amount ({{selected.currency}})<input type="number" min="0.01" step="0.01" [(ngModel)]="approvedAmount" /></label><div class="action-row"><button class="btn btn-primary" [disabled]="saving || !reason.trim() || approvedAmount<=0 || approvedAmount>selected.claimedAmount" (click)="execute(api.decide(selected.id,officerId,true,approvedAmount,reason),'Claim approved')">Approve</button><button class="btn btn-danger" [disabled]="saving || !reason.trim()" (click)="execute(api.decide(selected.id,officerId,false,null,reason),'Claim rejected')">Reject</button></div></div>
}
@if(selected.status==='NEEDS_INFORMATION'){<div class="note-panel">Awaiting customer response: {{selected.informationRequest}}</div>}
@if(selected.status==='APPROVED'){<div class="action-panel"><h3>Settlement</h3><p class="tiny-muted">Marks a demo status only. No payment integration.</p><button class="btn btn-primary" [disabled]="saving" (click)="execute(api.settle(selected.id,officerId),'Marked settled')">Mark settled</button></div>}
@if(selected.status==='REJECTED'||selected.status==='SETTLED'){<div class="note-panel">This claim is closed. No further workflow actions are allowed.</div>}
<h3 class="timeline-title">Audit trail</h3><div class="timeline">@for(a of activities;track a.id){<div class="timeline-item"><strong>{{status(a.action)}}</strong><small>{{a.createdAt | date:'short'}} · {{a.actorId}}</small><p>{{a.message}}</p></div>}</div>
}@else{<div class="empty">Select a claim to process.</div>}</aside></div>
`})
export class ClaimsQueueComponent implements OnInit {
 api = inject(ClaimApiService);officerId='officer-001';claims:Claim[]=[];selected:Claim|null=null;activities:ClaimActivity[]=[];
 error='';message='';loading=false;saving=false;infoMessage='';reason='';reserveAmount=0;approvedAmount=0;status=displayStatus;
 ngOnInit(){this.load();}
 load(){this.loading=true;this.error='';this.api.list().subscribe({next:claims=>{this.claims=claims;this.loading=false;if(this.selected){const found=claims.find(c=>c.id===this.selected?.id);if(found)this.select(found);}else if(claims.length)this.select(claims[0]);},error:e=>{this.error=errorMessage(e);this.loading=false;}});}
 select(c:Claim){this.selected=c;this.reason='';this.infoMessage='';this.reserveAmount=c.reserveAmount??c.claimedAmount;this.approvedAmount=c.approvedAmount??c.claimedAmount;this.api.history(c.id).subscribe({next:a=>this.activities=a,error:e=>this.error=errorMessage(e)});}
 execute(request:Observable<Claim>,success:string){this.saving=true;this.error='';this.message='';request.subscribe({next:c=>{this.saving=false;this.message=success;this.claims=this.claims.map(x=>x.id===c.id?c:x);this.select(c);},error:e=>{this.saving=false;this.error=errorMessage(e);}});}
}
