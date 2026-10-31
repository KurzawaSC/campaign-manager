import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';

import { Product } from '../api/models';

@Component({
  selector: 'app-confirm-delete-product-dialog',
  imports: [MatButtonModule, MatDialogModule],
  template: `
    <h2 mat-dialog-title>Delete product?</h2>
    <mat-dialog-content>
      <p>"{{ product.name }}" will be deleted. A product with campaigns cannot be deleted.</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button matButton [mat-dialog-close]="false">Cancel</button>
      <button matButton="filled" [mat-dialog-close]="true">Delete</button>
    </mat-dialog-actions>
  `,
})
export class ConfirmDeleteProductDialog {
  protected readonly product = inject<Product>(MAT_DIALOG_DATA);
}
