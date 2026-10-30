import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';

import { Account } from './models';

@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly http = inject(HttpClient);
  private readonly balanceState = signal<number | null>(null);

  readonly balance = this.balanceState.asReadonly();

  refresh(): void {
    this.http.get<Account>('/api/account').subscribe({
      next: (account) => this.balanceState.set(account.balance),
      error: () => this.balanceState.set(null),
    });
  }
}
