import { DecimalPipe } from '@angular/common';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { BehaviorSubject, catchError, EMPTY, filter, switchMap, tap } from 'rxjs';

import { problemMessage } from '../api/api-problem';
import { CampaignService } from '../api/campaign.service';
import { Campaign, Product } from '../api/models';
import { ProductService } from '../api/product.service';
import { ConfirmDeleteDialog } from './confirm-delete-dialog';

@Component({
  selector: 'app-campaign-list',
  imports: [
    DecimalPipe,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatProgressBarModule,
    MatSelectModule,
    MatTableModule,
    MatTooltipModule,
    RouterLink,
  ],
  templateUrl: './campaign-list.html',
  styleUrl: './campaign-list.scss',
})
export class CampaignList implements OnInit {
  private readonly campaignService = inject(CampaignService);
  private readonly productService = inject(ProductService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);
  private readonly selectedProduct = new BehaviorSubject<number | null>(null);

  protected readonly columns = ['name', 'product', 'keywords', 'bid', 'fund', 'status', 'town', 'radius', 'actions'];
  protected readonly campaigns = signal<Campaign[]>([]);
  protected readonly products = signal<Product[]>([]);
  protected readonly productFilter = signal<number | null>(null);
  protected readonly loading = signal(true);
  protected readonly loaded = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.productService
      .getProducts()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (products) => this.products.set(products),
        error: () => this.snackBar.open('Could not load the product filter.', 'Close', { duration: 5000 }),
      });

    this.selectedProduct
      .pipe(
        tap(() => {
          this.loading.set(true);
          this.error.set(null);
        }),
        switchMap((productId) =>
          this.campaignService.getCampaigns(productId).pipe(
            catchError((error) => {
              this.error.set(problemMessage(error));
              this.loading.set(false);
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((campaigns) => {
        this.campaigns.set(campaigns);
        this.loaded.set(true);
        this.loading.set(false);
      });
  }

  protected filterByProduct(productId: number | null): void {
    this.productFilter.set(productId);
    this.selectedProduct.next(productId);
  }

  protected reload(): void {
    this.selectedProduct.next(this.selectedProduct.value);
  }

  protected keywordNames(campaign: Campaign): string {
    return campaign.keywords.map((keyword) => keyword.name).join(', ');
  }

  protected confirmDelete(campaign: Campaign): void {
    this.dialog
      .open<ConfirmDeleteDialog, Campaign, boolean>(ConfirmDeleteDialog, { data: campaign })
      .afterClosed()
      .pipe(
        filter(Boolean),
        switchMap(() => this.campaignService.deleteCampaign(campaign.id)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.snackBar.open(`Campaign "${campaign.name}" deleted.`, 'Close', { duration: 4000 });
          this.reload();
        },
        error: (error) => {
          this.snackBar.open(problemMessage(error), 'Close', { duration: 6000 });
          this.reload();
        },
      });
  }
}
