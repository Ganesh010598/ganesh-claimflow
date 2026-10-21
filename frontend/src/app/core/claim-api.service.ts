import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';

export type ClaimStatus = 'SUBMITTED'|'IN_REVIEW'|'NEEDS_INFORMATION'|'APPROVED'|'REJECTED'|'SETTLED';
export type Market = 'MY'|'SG'|'HK'|'TH'|'VN'|'ID';
export type ClaimType = 'MOTOR'|'PROPERTY';

export interface CreateClaimRequest {
  claimantId: string; claimantName: string; claimType: ClaimType;
  market: Market; incidentDate: string; description: string; claimedAmount: number;
}
export interface Claim extends CreateClaimRequest {
  id: number; claimNumber: string; currency: string; reserveAmount: number|null;
  approvedAmount: number|null; status: ClaimStatus; assignedOfficerId: string|null;
  informationRequest: string|null; decisionReason: string|null;
  createdAt: string; updatedAt: string;
}
export interface ClaimsPage {
  content: Claim[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
  first: boolean;
  last: boolean;
}
export interface ClaimActivity {
  id: number; actorId: string; action: string; message: string;
  status: ClaimStatus; createdAt: string;
}
export interface Exposure { market: string; currency: string; amount: number; }
export interface Dashboard {
  totalClaims: number; openClaims: number; awaitingReview: number;
  awaitingCustomer: number; approvedUnsettled: number;
  exposureByMarket: Exposure[]; workloadByOfficer: Record<string, number>;
}

@Injectable({providedIn:'root'})
export class ClaimApiService {
  private http = inject(HttpClient);
  private readonly base = '/api/claims';

  create(body: CreateClaimRequest) { return this.http.post<Claim>(this.base, body); }
  get(id: number) { return this.http.get<Claim>(`${this.base}/${id}`); }
  list(claimantId?: string) {
    const params = claimantId ? new HttpParams().set('claimantId', claimantId) : new HttpParams();
    return this.http.get<Claim[]>(this.base, {params});
  }
  listPage(page = 0, size = 10, claimantId?: string, status?: ClaimStatus) {
    let params = new HttpParams().set('page', page).set('size', size);
    if (claimantId?.trim()) params = params.set('claimantId', claimantId.trim());
    if (status) params = params.set('status', status);
    return this.http.get<ClaimsPage>(this.base, {params});
  }
  history(id: number) { return this.http.get<ClaimActivity[]>(`${this.base}/${id}/history`); }
  assign(id: number, officerId: string) { return this.http.post<Claim>(`${this.base}/${id}/assign`, {officerId}); }
  reserve(id: number, officerId: string, amount: number) { return this.http.post<Claim>(`${this.base}/${id}/reserve`, {officerId, amount}); }
  requestInfo(id: number, officerId: string, message: string) { return this.http.post<Claim>(`${this.base}/${id}/request-info`, {officerId,message}); }
  provideInfo(id: number, message: string) { return this.http.post<Claim>(`${this.base}/${id}/information`, {message}); }
  decide(id: number, officerId: string, approved: boolean, approvedAmount: number|null, reason: string) {
    return this.http.post<Claim>(`${this.base}/${id}/decision`, {officerId, approved, approvedAmount, reason});
  }
  settle(id: number, officerId: string) { return this.http.post<Claim>(`${this.base}/${id}/settle`, {officerId}); }
  dashboard() { return this.http.get<Dashboard>('/api/dashboard'); }
}

export function displayStatus(value: string): string { return value.replaceAll('_',' ').toLowerCase().replace(/^./, s => s.toUpperCase()); }
export function errorMessage(error: unknown): string {
  const e = error as {error?: {error?: string}; status?: number};
  return e?.error?.error ?? (e?.status === 0 ? 'Cannot connect to the backend. Start Spring Boot on port 8080.' : 'Request failed. Check backend logs.');
}
