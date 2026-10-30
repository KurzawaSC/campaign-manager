import { DecimalPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';

import { Campaign } from '../api/models';

@Component({
  selector: 'app-confirm-delete-dialog',
  imports: [DecimalPipe, MatButtonModule, MatDialogModule],
  template: `
    <h2 mat-dialog-title>Delete campaign?</h2>
    <mat-dialog-content>
      <p>
        "{{ campaign.name }}" will be deleted and its fund of
        <strong>{{ campaign.campaignFund | number: '1.2-2' }}</strong> will return to your Emerald account.
      </p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button matButton [mat-dialog-close]="false">Cancel</button>
      <button matButton="filled" [mat-dialog-close]="true">Delete</button>
    </mat-dialog-actions>
  `,
})
export class ConfirmDeleteDialog {
  protected readonly campaign = inject<Campaign>(MAT_DIALOG_DATA);
}
