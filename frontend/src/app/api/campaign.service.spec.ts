import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AccountService } from './account.service';
import { CampaignService } from './campaign.service';
import { CampaignRequest } from './models';

const request: CampaignRequest = {
  name: 'Summer sale',
  productId: 1,
  keywordIds: [1],
  bidAmount: 1.5,
  campaignFund: 100,
  status: 'ON',
  townId: null,
  radiusKm: 25,
};

describe('CampaignService', () => {
  let service: CampaignService;
  let account: AccountService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CampaignService);
    account = TestBed.inject(AccountService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends the product filter only when one is chosen', () => {
    service.getCampaigns(null).subscribe();
    http.expectOne('/api/campaigns').flush([]);

    service.getCampaigns(3).subscribe();
    http.expectOne('/api/campaigns?productId=3').flush([]);
  });

  it('refreshes the balance after a successful change', () => {
    service.createCampaign(request).subscribe();
    http.expectOne('/api/campaigns').flush({ id: 1 });

    http.expectOne('/api/account').flush({ balance: 9900 });
    expect(account.balance()).toBe(9900);
  });

  it('refreshes the balance after a failed change', () => {
    service.updateCampaign(1, request).subscribe({ error: () => undefined });
    http.expectOne('/api/campaigns/1').flush({ detail: 'Insufficient funds' }, { status: 409, statusText: 'Conflict' });

    http.expectOne('/api/account').flush({ balance: 5 });
    expect(account.balance()).toBe(5);
  });

  it('refreshes the balance after a delete', () => {
    service.deleteCampaign(1).subscribe();
    http.expectOne('/api/campaigns/1').flush(null, { status: 204, statusText: 'No Content' });

    http.expectOne('/api/account').flush({ balance: 10000 });
    expect(account.balance()).toBe(10000);
  });
});
