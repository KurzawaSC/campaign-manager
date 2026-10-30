import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { finalize, Observable } from 'rxjs';

import { AccountService } from './account.service';
import { Campaign, CampaignRequest } from './models';

const CAMPAIGNS_URL = '/api/campaigns';

@Injectable({ providedIn: 'root' })
export class CampaignService {
  private readonly http = inject(HttpClient);
  private readonly accountService = inject(AccountService);

  getCampaigns(productId: number | null): Observable<Campaign[]> {
    const params = productId === null ? new HttpParams() : new HttpParams().set('productId', productId);
    return this.http.get<Campaign[]>(CAMPAIGNS_URL, { params });
  }

  getCampaign(id: number): Observable<Campaign> {
    return this.http.get<Campaign>(`${CAMPAIGNS_URL}/${id}`);
  }

  createCampaign(request: CampaignRequest): Observable<Campaign> {
    return this.refreshingBalance(this.http.post<Campaign>(CAMPAIGNS_URL, request));
  }

  updateCampaign(id: number, request: CampaignRequest): Observable<Campaign> {
    return this.refreshingBalance(this.http.put<Campaign>(`${CAMPAIGNS_URL}/${id}`, request));
  }

  deleteCampaign(id: number): Observable<void> {
    return this.refreshingBalance(this.http.delete<void>(`${CAMPAIGNS_URL}/${id}`));
  }

  private refreshingBalance<T>(request: Observable<T>): Observable<T> {
    return request.pipe(finalize(() => this.accountService.refresh()));
  }
}
