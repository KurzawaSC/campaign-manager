import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { problemMessage, toApiProblem } from '../api/api-problem';
import { ProductService } from '../api/product.service';
import { notBlank } from '../campaigns/campaign-validators';

@Component({
  selector: 'app-product-form',
  imports: [
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    ReactiveFormsModule,
    RouterLink,
  ],
  templateUrl: './product-form.html',
  styleUrl: './product-form.scss',
})
export class ProductForm implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);
  private readonly productService = inject(ProductService);
  private readonly routeId = this.route.snapshot.paramMap.get('id');

  protected readonly isEdit = this.routeId !== null;
  protected readonly loading = signal(this.isEdit);
  protected readonly saving = signal(false);
  protected readonly loadError = signal<string | null>(null);
  protected readonly saveError = signal<string | null>(null);

  protected readonly form = new FormGroup({
    name: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, notBlank, Validators.maxLength(255)],
    }),
  });
  protected readonly name = this.form.controls.name;

  ngOnInit(): void {
    this.name.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.saveError.set(null));
    if (this.isEdit) {
      this.load();
    }
  }

  protected nameError(): string {
    const errors = this.name.errors ?? {};
    if (errors['server']) {
      return errors['server'];
    }
    if (errors['required']) {
      return 'This field is required';
    }
    if (errors['maxlength']) {
      return `Must be at most ${errors['maxlength'].requiredLength} characters`;
    }
    return '';
  }

  protected save(): void {
    this.name.markAsTouched();
    if (this.name.invalid || this.saving()) {
      return;
    }
    this.saving.set(true);
    this.saveError.set(null);
    const request = { name: this.name.value.trim() };
    const save$ = this.isEdit
      ? this.productService.updateProduct(Number(this.routeId), request)
      : this.productService.createProduct(request);
    save$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (product) => {
        this.snackBar.open(`Product "${product.name}" saved.`, 'Close', { duration: 4000 });
        this.router.navigateByUrl('/products');
      },
      error: (error) => {
        this.saving.set(false);
        const nameError = toApiProblem(error).fieldErrors?.['name'];
        if (nameError) {
          this.name.setErrors({ server: nameError });
          this.name.markAsTouched();
        } else {
          this.saveError.set(problemMessage(error));
        }
      },
    });
  }

  private load(): void {
    const id = Number(this.routeId);
    if (!Number.isInteger(id)) {
      this.loading.set(false);
      this.loadError.set('Product not found.');
      return;
    }
    this.productService
      .getProduct(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (product) => {
          this.name.setValue(product.name);
          this.loading.set(false);
        },
        error: (error) => {
          this.loadError.set(problemMessage(error));
          this.loading.set(false);
        },
      });
  }
}
