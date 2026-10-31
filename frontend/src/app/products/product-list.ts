import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { filter, switchMap } from 'rxjs';

import { problemMessage } from '../api/api-problem';
import { Product } from '../api/models';
import { ProductService } from '../api/product.service';
import { ConfirmDeleteProductDialog } from './confirm-delete-product-dialog';

@Component({
  selector: 'app-product-list',
  imports: [MatButtonModule, MatIconModule, MatProgressBarModule, MatTableModule, MatTooltipModule, RouterLink],
  templateUrl: './product-list.html',
  styleUrl: './product-list.scss',
})
export class ProductList implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly columns = ['name', 'actions'];
  protected readonly products = signal<Product[]>([]);
  protected readonly loading = signal(true);
  protected readonly loaded = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.productService
      .getProducts()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (products) => {
          this.products.set(products);
          this.loaded.set(true);
          this.loading.set(false);
        },
        error: (error) => {
          this.error.set(problemMessage(error));
          this.loading.set(false);
        },
      });
  }

  protected confirmDelete(product: Product): void {
    this.dialog
      .open<ConfirmDeleteProductDialog, Product, boolean>(ConfirmDeleteProductDialog, { data: product })
      .afterClosed()
      .pipe(
        filter(Boolean),
        switchMap(() => this.productService.deleteProduct(product.id)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.snackBar.open(`Product "${product.name}" deleted.`, 'Close', { duration: 4000 });
          this.load();
        },
        error: (error) => {
          this.snackBar.open(problemMessage(error), 'Close', { duration: 6000 });
          this.load();
        },
      });
  }
}
