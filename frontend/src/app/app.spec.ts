import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { App } from './app';

describe('App', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    httpMock.expectOne('/currencies').flush([]);
    httpMock.expectOne('/currency?code=JPY').flush({ code: 'JPY', name: 'Japanese Yen', date: '2026-07-29', rates: [] });

    expect(fixture.componentInstance).toBeTruthy();
  });

  it('renders the hero headline', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    httpMock.expectOne('/currencies').flush([]);
    httpMock.expectOne('/currency?code=JPY').flush({ code: 'JPY', name: 'Japanese Yen', date: '2026-07-29', rates: [] });

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1.headline')?.textContent).toContain('Convert between currencies');
  });
});
