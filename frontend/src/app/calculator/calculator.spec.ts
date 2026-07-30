import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { Calculator } from './calculator';

describe('Calculator', () => {
  let component: Calculator;
  let fixture: ComponentFixture<Calculator>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Calculator],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(Calculator);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  function initWithDefaultCurrencies(): void {
    fixture.detectChanges(); // triggers ngOnInit -> getCurrencies()
    httpMock.expectOne('/currencies').flush([
      { code: 'JPY', name: 'Japanese Yen' },
      { code: 'EUR', name: 'Euro' },
      { code: 'USD', name: 'US Dollar' },
    ]);
    httpMock.expectOne('/currency?code=JPY').flush({
      code: 'JPY',
      name: 'Japanese Yen',
      date: '2026-07-29',
      rates: [
        { to: 'EUR', rate: 0.00537 },
        { to: 'USD', rate: 0.0067 },
      ],
    });
  }

  it('should create', () => {
    initWithDefaultCurrencies();
    expect(component).toBeTruthy();
  });

  it('computes the converted amount from the cached rate', () => {
    initWithDefaultCurrencies();

    expect(component['conversion']()?.resultLabel).toBe('1 JPY = 0.01 EUR');
  });

  it('recomputes when the amount changes, without any HTTP request', () => {
    initWithDefaultCurrencies();

    component['amount'].set(1000);

    expect(component['conversion']()?.resultLabel).toBe('1000 JPY = 5.37 EUR');
    httpMock.expectNone('/currency?code=JPY');
  });

  it('recomputes when "to" changes, without any HTTP request', () => {
    initWithDefaultCurrencies();

    component['to'].set('USD');

    expect(component['conversion']()?.resultLabel).toBe('1 JPY = 0.01 USD');
    httpMock.expectNone((req) => req.url.startsWith('/currency'));
  });

  it('fetches new rates when "from" changes', () => {
    initWithDefaultCurrencies();

    component['onFromChange']({ target: { value: 'USD' } } as unknown as Event);

    httpMock.expectOne('/currency?code=USD').flush({
      code: 'USD',
      name: 'US Dollar',
      date: '2026-07-29',
      rates: [{ to: 'EUR', rate: 0.9 }],
    });
    expect(component['currentRates']()?.code).toBe('USD');
  });

  it('shows an error message and clears rates when the rates request fails', () => {
    initWithDefaultCurrencies();

    component['onFromChange']({ target: { value: 'XXX' } } as unknown as Event);

    httpMock.expectOne('/currency?code=XXX').flush(
      { message: 'Unknown currency code: XXX' },
      { status: 400, statusText: 'Bad Request' },
    );

    expect(component['currentRates']()).toBeNull();
    expect(component['errorMessage']()).toBe('Unknown currency code: XXX');
  });
});
