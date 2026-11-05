import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';

describe('App', function () {
  beforeEach(async function () {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), provideHttpClient()],
    }).compileComponents();
  });

  it('crea la aplicación', function () {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('muestra el encabezado con el logo CARMIR y el carrito', async function () {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const pagina = fixture.nativeElement as HTMLElement;
    // El logo es la imagen de CARMIR tal cual
    expect(pagina.querySelector('.logo img')?.getAttribute('alt')).toContain('CARMIR');
    expect(pagina.querySelector('.enlace-carrito')?.textContent).toContain('Carrito');
  });
});
