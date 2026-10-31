import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Product, ProductRequest } from './models';

const PRODUCTS_URL = '/api/products';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);

  getProducts(): Observable<Product[]> {
    return this.http.get<Product[]>(PRODUCTS_URL);
  }

  getProduct(id: number): Observable<Product> {
    return this.http.get<Product>(`${PRODUCTS_URL}/${id}`);
  }

  createProduct(request: ProductRequest): Observable<Product> {
    return this.http.post<Product>(PRODUCTS_URL, request);
  }

  updateProduct(id: number, request: ProductRequest): Observable<Product> {
    return this.http.put<Product>(`${PRODUCTS_URL}/${id}`, request);
  }

  deleteProduct(id: number): Observable<void> {
    return this.http.delete<void>(`${PRODUCTS_URL}/${id}`);
  }
}
