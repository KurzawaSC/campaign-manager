import { Routes } from '@angular/router';

const loadCampaignForm = () => import('./campaigns/campaign-form').then((module) => module.CampaignForm);
const loadProductForm = () => import('./products/product-form').then((module) => module.ProductForm);

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'campaigns' },
  {
    path: 'campaigns',
    pathMatch: 'full',
    title: 'Campaigns',
    loadComponent: () => import('./campaigns/campaign-list').then((module) => module.CampaignList),
  },
  { path: 'campaigns/new', title: 'New campaign', loadComponent: loadCampaignForm },
  { path: 'campaigns/:id/edit', title: 'Edit campaign', loadComponent: loadCampaignForm },
  {
    path: 'products',
    pathMatch: 'full',
    title: 'Products',
    loadComponent: () => import('./products/product-list').then((module) => module.ProductList),
  },
  { path: 'products/new', title: 'New product', loadComponent: loadProductForm },
  { path: 'products/:id/edit', title: 'Edit product', loadComponent: loadProductForm },
  { path: '**', redirectTo: 'campaigns' },
];
