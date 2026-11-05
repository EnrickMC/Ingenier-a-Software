import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { CODIGO_SIN_CONEXION, CODIGO_SIN_CONEXION_BD } from '../../../../core/modelos/error-api';
import { CatalogoApiService } from '../../servicios/catalogo-api.service';
import CatalogoPagina from './catalogo-pagina';

// Servicio falso para no depender de la API
// fallo: '' = responde bien, 'api' = la API no responde, 'bd' = la API no llega a MySQL
function crearServicioFalso(productos: unknown[], fallo: string) {
  return {
    listarCategorias: async function () {
      return [];
    },
    listarProductos: async function () {
      if (fallo === 'api') {
        throw { estado: 0, codigo: CODIGO_SIN_CONEXION, mensaje: 'sin conexión', referencia: '' };
      }
      if (fallo === 'bd') {
        throw { estado: 503, codigo: CODIGO_SIN_CONEXION_BD, mensaje: 'sin base', referencia: 'abc123' };
      }
      return productos;
    },
  };
}

async function crearPagina(productos: unknown[], fallo: string): Promise<HTMLElement> {
  await TestBed.configureTestingModule({
    imports: [CatalogoPagina],
    providers: [provideRouter([]), { provide: CatalogoApiService, useValue: crearServicioFalso(productos, fallo) }],
  }).compileComponents();
  const fixture = TestBed.createComponent(CatalogoPagina);
  fixture.detectChanges();
  await fixture.whenStable();
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

describe('CatalogoPagina (RF-1)', function () {
  it('muestra los productos en la cuadrícula', async function () {
    const pagina = await crearPagina(
      [{ idPostre: 1, nombre: 'Pastel', descripcion: null, linkFotografia: null, precio: 100, idCategoria: 1, nombreCategoria: 'Pastel' }],
      '',
    );
    expect(pagina.querySelectorAll('app-tarjeta-producto').length).toBe(1);
  });

  it('sin productos muestra «Aún no hay productos disponibles»', async function () {
    const pagina = await crearPagina([], '');
    expect(pagina.textContent).toContain('Aún no hay productos disponibles');
  });

  it('si la API no responde lo dice y muestra «Reintentar»', async function () {
    const pagina = await crearPagina([], 'api');
    expect(pagina.textContent).toContain('No fue posible cargar el catálogo');
    expect(pagina.textContent).toContain('La API no responde');
    expect(pagina.textContent).toContain('Reintentar');
  });

  it('si la API no llega a MySQL lo distingue de la API apagada', async function () {
    const pagina = await crearPagina([], 'bd');
    expect(pagina.textContent).toContain('No fue posible cargar el catálogo');
    expect(pagina.textContent).toContain('no pudo conectarse a MySQL');
    expect(pagina.textContent).not.toContain('La API no responde');
  });
});
