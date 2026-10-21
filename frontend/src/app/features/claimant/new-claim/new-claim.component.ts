import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ClaimApiService, Claim, CreateClaimRequest, Market, errorMessage } from '../../../core/claim-api.service';

@Component({
 selector:'app-new-claim', standalone:true, imports:[FormsModule, RouterLink],
 template: `
 <div class="page-heading"><div><h1>New claim</h1><p>Enter the details of your incident.</p></div></div>
 @if (created) {
   <section class="card success-card" aria-live="polite"><div class="success-mark">✓</div><h2>Claim submitted successfully</h2><p>Save this reference number to track your claim.</p>
     <div class="claim-reference">{{created.claimNumber}}</div><div class="action-row"><a class="btn btn-primary" routerLink="/my-claims">Track my claims →</a><button class="btn btn-secondary" type="button" (click)="reset()">Submit another</button></div>
   </section>
 } @else {
   <div class="two-column"><section class="card"><div class="section-title"><div><h2>Claim details</h2></div></div>
     <form #f="ngForm" (ngSubmit)="submit()">
       <div class="form-grid">
         <label>Customer ID <span>*</span><input name="claimantId" required [(ngModel)]="form.claimantId" /></label>
         <label>Your name <span>*</span><input name="claimantName" required [(ngModel)]="form.claimantName" /></label>
         <label>Insurance type <span>*</span><select name="claimType" [(ngModel)]="form.claimType"><option value="MOTOR">Motor insurance</option><option value="PROPERTY">Property insurance</option></select></label>
         <label>Market <span>*</span><select name="market" [(ngModel)]="form.market"><option value="MY">Malaysia (MYR)</option><option value="SG">Singapore (SGD)</option><option value="HK">Hong Kong (HKD)</option><option value="TH">Thailand (THB)</option><option value="VN">Vietnam (VND)</option><option value="ID">Indonesia (IDR)</option></select></label>
         <label>Incident date <span>*</span><input type="date" name="incidentDate" required [max]="today" [(ngModel)]="form.incidentDate" /></label>
         <label>Claimed amount <span>*</span><input type="number" name="claimedAmount" min="0.01" step="0.01" required [(ngModel)]="form.claimedAmount" /></label>
       </div>
       <label>Describe what happened <span>*</span><textarea name="description" required minlength="5" maxlength="2000" rows="5" placeholder="What happened, where, and what was damaged?" [(ngModel)]="form.description"></textarea></label>
       @if (error) {<div class="alert error" role="alert">{{error}}</div>}
       <div class="form-actions"><span class="tiny-muted">Fields marked * are required.</span><button class="btn btn-primary" type="submit" [disabled]="f.invalid || loading">{{loading ? 'Submitting…' : 'Submit claim →'}}</button></div>
     </form>
   </section><aside class="side-stack"><div class="info-panel"><div class="small-icon">✳</div><h3>What happens next</h3><ol><li>We register your claim.</li><li>An officer picks it up for review.</li><li>You'll see status changes in My Claims.</li><li>We communicate the outcome here.</li></ol></div><div class="note-panel">Demo only. No real insurance claim will be created.</div></aside></div>
 }
 `
})
export class NewClaimComponent {
  private api = inject(ClaimApiService);
  today = new Date().toLocaleDateString('en-CA');
  form: CreateClaimRequest = {claimantId:'demo-customer-001',claimantName:'Alex Tan',claimType:'MOTOR',market:'MY' as Market,incidentDate:'',description:'',claimedAmount:3500};
  created: Claim|null = null;
  loading = false;
  error = '';
  submit() {
    this.loading = true; this.error = '';
    this.api.create(this.form).subscribe({next: claim => {this.created=claim;this.loading=false;},error: err => {this.error=errorMessage(err);this.loading=false;}});
  }
  reset() {this.created=null;this.form.description='';this.form.incidentDate='';}
}
