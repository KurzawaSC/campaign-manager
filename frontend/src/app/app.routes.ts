import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Campaigns',
    loadComponent: () => import('./campaigns/campaign-list').then((module) => module.CampaignList),
  },
  { path: '**', redirectTo: '' },
];
