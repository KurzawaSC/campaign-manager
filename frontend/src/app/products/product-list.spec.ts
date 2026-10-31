import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { ProductList } from './product-list';

const mug = { id: 1, name: 'Travel Mug' };

describe('ProductList', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<ProductList>;
  let component: ProductList;
  let dialogResult: boolean;
  const snackBar = { open: vi.fn() };

  beforeEach(async () => {
    dialogResult = true;
    snackBar.open.mockClear();
    await TestBed.configureTestingModule({
      imports: [ProductList],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MatDialog, useValue: { open: () => ({ afterClosed: () => of(dialogResult) }) } },
        { provide: MatSnackBar, useValue: snackBar },
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ProductList);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function text(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  it('shows the products', async () => {
    http.expectOne('/api/products').flush([mug]);
    await fixture.whenStable();

    expect(text()).toContain('Travel Mug');
  });

  it('shows a message when there are no products', async () => {
    http.expectOne('/api/products').flush([]);
    await fixture.whenStable();

    expect(text()).toContain('No products yet');
  });

  it('deletes a confirmed product and reloads the list', () => {
    http.expectOne('/api/products').flush([mug]);

    component['confirmDelete'](mug);

    http.expectOne({ method: 'DELETE', url: '/api/products/1' }).flush(null, { status: 204, statusText: 'No Content' });
    http.expectOne({ method: 'GET', url: '/api/products' }).flush([]);
    expect(snackBar.open.mock.calls[0][0]).toBe('Product "Travel Mug" deleted.');
  });

  it('does not delete when the dialog is cancelled', () => {
    http.expectOne('/api/products').flush([mug]);
    dialogResult = false;

    component['confirmDelete'](mug);

    http.expectNone({ method: 'DELETE', url: '/api/products/1' });
  });

  it('shows why a product with campaigns cannot be deleted', () => {
    http.expectOne('/api/products').flush([mug]);
    const detail = 'Product "Travel Mug" has campaigns. Delete its campaigns first.';

    component['confirmDelete'](mug);

    http.expectOne({ method: 'DELETE', url: '/api/products/1' }).flush({ detail }, { status: 409, statusText: 'Conflict' });
    http.expectOne({ method: 'GET', url: '/api/products' }).flush([mug]);
    expect(snackBar.open.mock.calls[0][0]).toBe(detail);
  });
});
