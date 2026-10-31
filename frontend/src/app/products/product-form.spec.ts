import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';

import { ProductForm } from './product-form';

describe('ProductForm', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<ProductForm>;
  let component: ProductForm;

  async function setUp(id: string | null): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [ProductForm],
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
    fixture = TestBed.createComponent(ProductForm);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('does not send a blank name', async () => {
    await setUp(null);
    component['name'].setValue('   ');

    component['save']();

    http.expectNone('/api/products');
    expect(component['nameError']()).toBe('This field is required');
  });

  it('saves when the save button is clicked', async () => {
    await setUp(null);
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    await fixture.whenStable();
    component['name'].setValue('Travel Mug');

    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button[type="submit"]')?.click();

    http.expectOne({ method: 'POST', url: '/api/products' }).flush({ id: 1, name: 'Travel Mug' });
  });

  it('sends the trimmed name and returns to the list', async () => {
    await setUp(null);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    component['name'].setValue('  Travel Mug  ');

    component['save']();

    const call = http.expectOne((request) => request.method === 'POST' && request.url === '/api/products');
    expect(call.request.body).toEqual({ name: 'Travel Mug' });
    call.flush({ id: 1, name: 'Travel Mug' });
    expect(navigate).toHaveBeenCalledWith('/products');
  });

  it('shows a duplicate name error on the field and clears it after a change', async () => {
    await setUp(null);
    component['name'].setValue('Travel Mug');

    component['save']();
    http
      .expectOne('/api/products')
      .flush(
        { detail: 'Duplicate', fieldErrors: { name: 'A product named "Travel Mug" already exists' } },
        { status: 409, statusText: 'Conflict' },
      );

    expect(component['nameError']()).toBe('A product named "Travel Mug" already exists');
    expect(component['saveError']()).toBeNull();
    expect(component['saving']()).toBe(false);

    component['name'].setValue('Thermal Mug');
    expect(component['name'].valid).toBe(true);
  });

  it('loads and updates an existing product', async () => {
    await setUp('5');
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    http.expectOne('/api/products/5').flush({ id: 5, name: 'Travel Mug' });
    expect(component['name'].value).toBe('Travel Mug');

    component['name'].setValue('Thermal Mug');
    component['save']();

    const call = http.expectOne((request) => request.method === 'PUT');
    expect(call.request.url).toBe('/api/products/5');
    expect(call.request.body).toEqual({ name: 'Thermal Mug' });
    call.flush({ id: 5, name: 'Thermal Mug' });
  });

  it('shows a message for a product that cannot be loaded', async () => {
    await setUp('5');
    http.expectOne('/api/products/5').flush({ detail: 'Product 5 not found' }, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Product 5 not found');
  });

  it('does not call the API for an invalid id', async () => {
    await setUp('abc');
    await fixture.whenStable();

    http.expectNone('/api/products/NaN');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Product not found.');
  });
});
