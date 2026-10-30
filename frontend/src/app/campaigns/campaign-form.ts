import { DecimalPipe } from '@angular/common';
import { Component, computed, DestroyRef, effect, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, debounceTime, distinctUntilChanged, filter, forkJoin, of, startWith, switchMap } from 'rxjs';

import { AccountService } from '../api/account.service';
import { problemMessage } from '../api/api-problem';
import { CampaignService } from '../api/campaign.service';
import { ConfigService } from '../api/config.service';
import { DictionaryService } from '../api/dictionary.service';
import { Campaign, CampaignRequest, CampaignStatus, Keyword, Product, Town } from '../api/models';
import { ProductService } from '../api/product.service';
import { moneyFormat, notBlank, wholeNumber } from './campaign-validators';

@Component({
  selector: 'app-campaign-form',
  imports: [
    DecimalPipe,
    MatAutocompleteModule,
    MatButtonModule,
    MatChipsModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatRadioModule,
    MatSelectModule,
    ReactiveFormsModule,
    RouterLink,
  ],
  templateUrl: './campaign-form.html',
  styleUrl: './campaign-form.scss',
})
export class CampaignForm implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);
  private readonly accountService = inject(AccountService);
  private readonly campaignService = inject(CampaignService);
  private readonly configService = inject(ConfigService);
  private readonly dictionaryService = inject(DictionaryService);
  private readonly productService = inject(ProductService);
  private readonly routeId = this.route.snapshot.paramMap.get('id');

  protected readonly isEdit = this.routeId !== null;
  protected readonly products = signal<Product[]>([]);
  protected readonly towns = signal<Town[]>([]);
  protected readonly minBid = signal<number | null>(null);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly loadError = signal<string | null>(null);
  protected readonly saveError = signal<string | null>(null);
  protected readonly selectedKeywords = signal<Keyword[]>([]);

  private readonly suggestions = signal<Keyword[]>([]);
  private readonly currentFund = signal(0);

  protected readonly availableSuggestions = computed(() => {
    const selectedIds = new Set(this.selectedKeywords().map((keyword) => keyword.id));
    return this.suggestions().filter((keyword) => !selectedIds.has(keyword.id));
  });

  protected readonly availableFunds = computed(() => {
    const balance = this.accountService.balance();
    return balance === null ? null : balance + this.currentFund();
  });

  protected readonly keywordQuery = new FormControl<string | Keyword>('', { nonNullable: true });

  protected readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required, notBlank, Validators.maxLength(255)] }),
    productId: new FormControl<number | null>(null, Validators.required),
    keywordIds: new FormControl<Keyword[]>([], { nonNullable: true, validators: Validators.required }),
    bidAmount: new FormControl<number | null>(null, [Validators.required, moneyFormat]),
    campaignFund: new FormControl<number | null>(null, [
      Validators.required,
      Validators.min(0.01),
      moneyFormat,
      (control) => this.withinAvailableFunds(control),
    ]),
    status: new FormControl<CampaignStatus>('ON', { nonNullable: true, validators: Validators.required }),
    townId: new FormControl<number | null>(null),
    radiusKm: new FormControl<number | null>(null, [Validators.required, Validators.min(1), wholeNumber]),
  });

  constructor() {
    effect(() => {
      this.availableFunds();
      this.form.controls.campaignFund.updateValueAndValidity();
    });
  }

  ngOnInit(): void {
    this.load();
    this.keywordQuery.valueChanges
      .pipe(
        filter((value): value is string => typeof value === 'string'),
        startWith(''),
        debounceTime(250),
        distinctUntilChanged(),
        switchMap((query) => this.dictionaryService.findKeywords(query).pipe(catchError(() => of([])))),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((keywords) => this.suggestions.set(keywords));
  }

  protected addKeyword(event: MatAutocompleteSelectedEvent, input: HTMLInputElement): void {
    this.setKeywords([...this.selectedKeywords(), event.option.value]);
    input.value = '';
    this.keywordQuery.setValue('');
  }

  protected removeKeyword(keyword: Keyword): void {
    this.setKeywords(this.selectedKeywords().filter((selected) => selected.id !== keyword.id));
  }

  protected errorMessage(control: AbstractControl): string {
    const errors: ValidationErrors = control.errors ?? {};
    if (errors['required']) {
      return 'This field is required';
    }
    if (errors['min']) {
      return `Must be at least ${errors['min'].min}`;
    }
    if (errors['maxlength']) {
      return `Must be at most ${errors['maxlength'].requiredLength} characters`;
    }
    if (errors['money']) {
      return 'Enter an amount with up to 2 decimal places';
    }
    if (errors['wholeNumber']) {
      return 'Enter a whole number';
    }
    if (errors['insufficientFunds']) {
      return 'Exceeds your available funds';
    }
    return '';
  }

  protected save(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.saving()) {
      return;
    }
    this.saving.set(true);
    this.saveError.set(null);
    const request = this.toRequest();
    const save$ = this.isEdit
      ? this.campaignService.updateCampaign(Number(this.routeId), request)
      : this.campaignService.createCampaign(request);
    save$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (campaign) => {
        this.snackBar.open(`Campaign "${campaign.name}" saved.`, 'Close', { duration: 4000 });
        this.router.navigateByUrl('/');
      },
      error: (error) => {
        this.saving.set(false);
        this.saveError.set(problemMessage(error));
      },
    });
  }

  private load(): void {
    const id = Number(this.routeId);
    if (this.isEdit && !Number.isInteger(id)) {
      this.loading.set(false);
      this.loadError.set('Campaign not found.');
      return;
    }
    forkJoin({
      products: this.productService.getProducts(),
      towns: this.dictionaryService.getTowns(),
      config: this.configService.getConfig(),
      campaign: this.isEdit ? this.campaignService.getCampaign(id) : of(null),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ products, towns, config, campaign }) => {
          this.products.set(products);
          this.towns.set(towns);
          this.minBid.set(config.minBidAmount);
          this.form.controls.bidAmount.addValidators(Validators.min(config.minBidAmount));
          this.form.controls.bidAmount.updateValueAndValidity();
          if (campaign) {
            this.fillForm(campaign);
          }
          this.loading.set(false);
        },
        error: (error) => {
          this.loadError.set(problemMessage(error));
          this.loading.set(false);
        },
      });
  }

  private fillForm(campaign: Campaign): void {
    this.currentFund.set(campaign.campaignFund);
    this.setKeywords(campaign.keywords);
    this.form.patchValue({
      name: campaign.name,
      productId: campaign.product.id,
      bidAmount: campaign.bidAmount,
      campaignFund: campaign.campaignFund,
      status: campaign.status,
      townId: campaign.town?.id ?? null,
      radiusKm: campaign.radiusKm,
    });
  }

  private setKeywords(keywords: Keyword[]): void {
    this.selectedKeywords.set(keywords);
    this.form.controls.keywordIds.setValue(keywords);
    this.form.controls.keywordIds.markAsTouched();
  }

  private toRequest(): CampaignRequest {
    const value = this.form.getRawValue();
    return {
      name: value.name.trim(),
      productId: value.productId!,
      keywordIds: value.keywordIds.map((keyword) => keyword.id),
      bidAmount: value.bidAmount!,
      campaignFund: value.campaignFund!,
      status: value.status,
      townId: value.townId,
      radiusKm: value.radiusKm!,
    };
  }

  private withinAvailableFunds(control: AbstractControl): ValidationErrors | null {
    const available = this.availableFunds();
    return available !== null && control.value !== null && control.value > available
      ? { insufficientFunds: true }
      : null;
  }
}
