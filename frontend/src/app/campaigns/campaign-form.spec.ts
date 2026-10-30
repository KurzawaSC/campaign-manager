import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatChipGrid } from '@angular/material/chips';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';

import { AccountService } from '../api/account.service';
import { Campaign } from '../api/models';
import { CampaignForm } from './campaign-form';

const keyword = { id: 7, name: 'wireless' };

const campaign: Campaign = {
  id: 5,
  name: 'Winter sale',
  product: { id: 1, name: 'Smart Watch' },
  keywords: [keyword],
  bidAmount: 1.5,
  campaignFund: 200,
  status: 'OFF',
  town: { id: 2, name: 'Gdansk' },
  radiusKm: 30,
};

describe('CampaignForm', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<CampaignForm>;
  let component: CampaignForm;

  async function setUp(id: string | null): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [CampaignForm],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap(id === null ? {} : { id }) } },
        },
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CampaignForm);
    component = fixture.componentInstance;
    fixture.detectChanges();
    http.expectOne('/api/products').flush([{ id: 1, name: 'Smart Watch' }]);
    http.expectOne('/api/towns').flush([{ id: 2, name: 'Gdansk' }]);
    http.expectOne('/api/config').flush({ minBidAmount: 0.1 });
  }

  function fillValidForm(): void {
    component['setKeywords']([keyword]);
    component['form'].patchValue({
      name: '  Summer sale  ',
      productId: 1,
      bidAmount: 1.5,
      campaignFund: 100,
      radiusKm: 25,
    });
  }

  function text(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  it('does not send an invalid form and marks required fields', async () => {
    await setUp(null);

    component['save']();

    http.expectNone('/api/campaigns');
    const controls = component['form'].controls;
    expect(controls.name.hasError('required')).toBe(true);
    expect(controls.keywordIds.hasError('required')).toBe(true);
    expect(controls.radiusKm.touched).toBe(true);
  });

  it('applies the minimum bid from the API', async () => {
    await setUp(null);
    const bid = component['form'].controls.bidAmount;

    bid.setValue(0.09);
    expect(component['errorMessage'](bid)).toBe('Must be at least 0.10');

    bid.setValue(0.1);
    expect(bid.valid).toBe(true);
  });

  it('sends the trimmed request and returns to the list', async () => {
    await setUp(null);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    fillValidForm();

    component['save']();

    const call = http.expectOne((request) => request.method === 'POST' && request.url === '/api/campaigns');
    expect(call.request.body).toEqual({
      name: 'Summer sale',
      productId: 1,
      keywordIds: [7],
      bidAmount: 1.5,
      campaignFund: 100,
      status: 'ON',
      townId: null,
      radiusKm: 25,
    });
    call.flush({ ...campaign, name: 'Summer sale' });
    expect(navigate).toHaveBeenCalledWith('/');
  });

  it('sends the selected keywords after the chips are rendered', async () => {
    await setUp(null);
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    await fixture.whenStable();
    fillValidForm();
    fixture.detectChanges();
    await fixture.whenStable();
    const chipGrid = fixture.debugElement.query(By.directive(MatChipGrid)).componentInstance;
    chipGrid['_propagateChanges']();

    expect(component['form'].controls.keywordIds.value).toEqual([keyword]);
    component['save']();

    const call = http.expectOne((request) => request.method === 'POST' && request.url === '/api/campaigns');
    expect(call.request.body.keywordIds).toEqual([7]);
    call.flush(campaign);
  });

  it('shows API errors for nested fields on the parent field', async () => {
    await setUp(null);
    fillValidForm();

    component['save']();
    http
      .expectOne((request) => request.method === 'POST')
      .flush(
        { detail: 'Validation failed', fieldErrors: { 'keywordIds[].<iterable element>': 'must not be null' } },
        { status: 400, statusText: 'Bad Request' },
      );

    expect(component['errorMessage'](component['form'].controls.keywordIds)).toBe('must not be null');
    expect(component['saveError']()).toBe('Check the highlighted fields.');
  });

  it('shows API field errors on the matching fields', async () => {
    await setUp(null);
    fillValidForm();

    component['save']();
    http
      .expectOne((request) => request.method === 'POST')
      .flush(
        { detail: 'Validation failed', fieldErrors: { name: 'must not be blank', keywordIds: 'Some keywords were not found' } },
        { status: 400, statusText: 'Bad Request' },
      );

    const controls = component['form'].controls;
    expect(component['errorMessage'](controls.name)).toBe('must not be blank');
    expect(component['errorMessage'](controls.keywordIds)).toBe('Some keywords were not found');
    expect(component['saveError']()).toBe('Check the highlighted fields.');
    expect(component['saving']()).toBe(false);
  });

  it('shows the API message when funds are insufficient', async () => {
    await setUp(null);
    fillValidForm();

    component['save']();
    http
      .expectOne((request) => request.method === 'POST')
      .flush({ detail: 'Insufficient funds: available 5.00, required 100.00' }, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();

    expect(text()).toContain('Insufficient funds: available 5.00, required 100.00');

    http.expectOne('/api/account').flush({ balance: 5 });
    await fixture.whenStable();
    expect(component['saveError']()).not.toBeNull();

    component['form'].controls.campaignFund.setValue(5);
    expect(component['saveError']()).toBeNull();
  });

  it('loads a campaign for editing and counts its fund as available', async () => {
    await setUp('5');
    http.expectOne('/api/campaigns/5').flush(campaign);
    TestBed.inject(AccountService).refresh();
    http.expectOne('/api/account').flush({ balance: 50 });

    const form = component['form'];
    expect(form.controls.name.value).toBe('Winter sale');
    expect(form.controls.townId.value).toBe(2);
    expect(form.controls.keywordIds.value).toEqual([keyword]);
    expect(component['availableFunds']()).toBe(250);

    form.controls.campaignFund.setValue(250);
    expect(form.controls.campaignFund.valid).toBe(true);
    form.controls.campaignFund.setValue(250.01);
    expect(form.controls.campaignFund.hasError('insufficientFunds')).toBe(true);
  });

  it('updates an existing campaign', async () => {
    await setUp('5');
    http.expectOne('/api/campaigns/5').flush(campaign);
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    component['save']();

    const call = http.expectOne((request) => request.method === 'PUT');
    expect(call.request.url).toBe('/api/campaigns/5');
    expect(call.request.body.status).toBe('OFF');
    call.flush(campaign);
  });

  it('shows a message for a campaign that cannot be loaded', async () => {
    await setUp('5');
    http
      .expectOne('/api/campaigns/5')
      .flush({ detail: 'Campaign 5 not found' }, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();

    expect(text()).toContain('Campaign 5 not found');
  });
});
