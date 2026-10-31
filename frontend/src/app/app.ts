import { DecimalPipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AccountService } from './api/account.service';

@Component({
  selector: 'app-root',
  imports: [DecimalPipe, MatIconModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App implements OnInit {
  private readonly accountService = inject(AccountService);

  protected readonly balance = this.accountService.balance;

  ngOnInit(): void {
    this.accountService.refresh();
  }
}
