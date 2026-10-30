import { Routes } from '@angular/router';

const loadCampaignForm = () => import('./campaigns/campaign-form').then((module) => module.CampaignForm);

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Campaigns',
    loadComponent: () => import('./campaigns/campaign-list').then((module) => module.CampaignList),
  },
  { path: 'campaigns/new', title: 'New campaign', loadComponent: loadCampaignForm },
  { path: 'campaigns/:id/edit', title: 'Edit campaign', loadComponent: loadCampaignForm },
  { path: '**', redirectTo: '' },
];
