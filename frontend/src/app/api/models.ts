export type CampaignStatus = 'ON' | 'OFF';

export interface Product {
  id: number;
  name: string;
}

export interface ProductRequest {
  name: string;
}

export interface Keyword {
  id: number;
  name: string;
}

export interface Town {
  id: number;
  name: string;
}

export interface Account {
  balance: number;
}

export interface AppConfig {
  minBidAmount: number;
}

export interface Campaign {
  id: number;
  name: string;
  product: Product;
  keywords: Keyword[];
  bidAmount: number;
  campaignFund: number;
  status: CampaignStatus;
  town: Town | null;
  radiusKm: number;
}

export interface CampaignRequest {
  name: string;
  productId: number;
  keywordIds: number[];
  bidAmount: number;
  campaignFund: number;
  status: CampaignStatus;
  townId: number | null;
  radiusKm: number;
}

export interface ApiProblem {
  detail?: string;
  fieldErrors?: Record<string, string>;
}
