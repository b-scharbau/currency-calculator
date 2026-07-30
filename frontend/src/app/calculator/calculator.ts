import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CurrencyApi, Currency, CurrencyRates } from '../currency-api';

const POWERS_OF_TEN = [1, 10, 100, 1000, 10000, 100000, 1000000];

@Component({
  selector: 'app-calculator',
  imports: [],
  templateUrl: './calculator.html',
  styleUrl: './calculator.css',
})
export class Calculator implements OnInit {
  private readonly currencyApi = inject(CurrencyApi);

  protected readonly currencies = signal<Currency[]>([]);
  protected readonly from = signal('JPY');
  protected readonly to = signal('EUR');
  protected readonly amount = signal(1);
  protected readonly currentRates = signal<CurrencyRates | null>(null);
  protected readonly errorMessage = signal<string | null>(null);

  // Derived from currentRates()/to()/amount() only — no HTTP call, so typing an amount or
  // changing "to" is instant and reuses whatever /currency?code=<from> already returned.
  protected readonly conversion = computed(() => {
    const rates = this.currentRates();
    const to = this.to();
    const amount = this.amount();
    if (!rates || !to || !Number.isFinite(amount)) {
      return null;
    }
    const entry = rates.rates.find((r) => r.to === to);
    if (!entry) {
      return null;
    }
    const convertedAmount = amount * entry.rate;
    return {
      resultLabel: `${amount} ${rates.code} = ${convertedAmount.toFixed(2)} ${to}`,
      rateLabel: `1 ${rates.code} = ${entry.rate.toFixed(6)} ${to}`,
      date: rates.date,
    };
  });

  protected readonly powersOfTen = computed(() => {
    const rates = this.currentRates();
    const to = this.to();
    if (!rates || !to) {
      return null;
    }
    const entry = rates.rates.find((r) => r.to === to);
    if (!entry) {
      return null;
    }
    return {
      from: rates.code,
      to,
      rows: POWERS_OF_TEN.map((power) => ({
        powerLabel: power.toLocaleString(),
        convertedLabel: (power * entry.rate).toLocaleString(undefined, { maximumFractionDigits: 4 }),
      })),
    };
  });

  ngOnInit(): void {
    this.currencyApi.getCurrencies().subscribe({
      next: (currencies) => {
        this.currencies.set(currencies);
        this.fetchRatesForFrom();
      },
      error: (err) => this.errorMessage.set(`Could not load currency list: ${errorText(err)}`),
    });
  }

  protected onAmountInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.amount.set(input.checkValidity() ? input.valueAsNumber : NaN);
  }

  protected onFromChange(event: Event): void {
    this.from.set((event.target as HTMLSelectElement).value);
    this.fetchRatesForFrom();
  }

  protected onToChange(event: Event): void {
    this.to.set((event.target as HTMLSelectElement).value);
  }

  protected onSwap(): void {
    const previousFrom = this.from();
    this.from.set(this.to());
    this.to.set(previousFrom);
    this.fetchRatesForFrom();
  }

  protected onSubmit(event: Event): void {
    event.preventDefault();
  }

  private fetchRatesForFrom(): void {
    const from = this.from();
    if (!from) {
      return;
    }
    this.currencyApi.getCurrencyRates(from).subscribe({
      next: (rates) => {
        this.currentRates.set(rates);
        this.errorMessage.set(null);
      },
      error: (err) => {
        this.currentRates.set(null);
        this.errorMessage.set(errorText(err));
      },
    });
  }
}

function errorText(err: unknown): string {
  if (err && typeof err === 'object' && 'error' in err) {
    const body = (err as { error?: unknown }).error;
    if (body && typeof body === 'object' && 'message' in body && typeof (body as { message?: unknown }).message === 'string') {
      return (body as { message: string }).message;
    }
  }
  if (err && typeof err === 'object' && 'message' in err && typeof (err as { message?: unknown }).message === 'string') {
    return (err as { message: string }).message;
  }
  return 'Unknown error';
}
