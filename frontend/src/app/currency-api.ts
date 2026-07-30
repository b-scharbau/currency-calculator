import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface Currency {
  code: string;
  name: string;
}

export interface RateEntry {
  to: string;
  rate: number;
}

export interface CurrencyRates {
  code: string;
  name: string;
  date: string;
  rates: RateEntry[];
}

@Injectable({
  providedIn: 'root',
})
export class CurrencyApi {
  constructor(private readonly http: HttpClient) {}

  getCurrencies(): Observable<Currency[]> {
    return this.http.get<Currency[]>('/currencies');
  }

  getCurrencyRates(code: string): Observable<CurrencyRates> {
    return this.http.get<CurrencyRates>(`/currency?code=${code}`);
  }
}
