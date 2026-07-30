import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { CurrencyApi } from './currency-api';

describe('CurrencyApi', () => {
  let service: CurrencyApi;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CurrencyApi);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('fetches the currency list', () => {
    let result: unknown;
    service.getCurrencies().subscribe((currencies) => (result = currencies));

    const req = httpMock.expectOne('/currencies');
    expect(req.request.method).toBe('GET');
    req.flush([{ code: 'EUR', name: 'Euro' }]);

    expect(result).toEqual([{ code: 'EUR', name: 'Euro' }]);
  });

  it('fetches rates for a specific currency code', () => {
    let result: unknown;
    service.getCurrencyRates('usd').subscribe((rates) => (result = rates));

    const req = httpMock.expectOne('/currency?code=usd');
    expect(req.request.method).toBe('GET');
    req.flush({ code: 'USD', name: 'US Dollar', date: '2026-07-29', rates: [{ to: 'EUR', rate: 0.9 }] });

    expect(result).toEqual({
      code: 'USD',
      name: 'US Dollar',
      date: '2026-07-29',
      rates: [{ to: 'EUR', rate: 0.9 }],
    });
  });
});
