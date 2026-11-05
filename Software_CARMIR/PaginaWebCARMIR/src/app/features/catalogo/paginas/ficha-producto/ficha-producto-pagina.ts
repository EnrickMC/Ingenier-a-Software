import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, ParamMap, Router, RouterLink } from '@angular/router';
import { AvisosService } from '../../../../core/servicios/avisos.service';
import { explicarFalla, tieneCodigo } from '../../../../core/utilidades/errores';
import { Cargando } from '../../../../shared/componentes/cargando/cargando';
import { ImagenPostre } from '../../../../shared/componentes/imagen-postre/imagen-postre';
import { MensajeEstado } from '../../../../shared/componentes/mensaje-estado/mensaje-estado';
import { FichaProducto, GrupoIncluido } from '../../modelos/catalogo.models';
import { CatalogoApiService } from '../../servicios/catalogo-api.service';

type EstadoPantalla = 'cargando' | 'listo' | 'error';

/**
 * RF-2 · Ficha detallada del producto (plantilla base):
 * foto, descripción, precio, lo que incluye la plantilla, ingredientes y lo que se puede personalizar.
 */
@Component({
  selector: 'app-ficha-producto-pagina',
  imports: [RouterLink, CurrencyPipe, ImagenPostre, MensajeEstado, Cargando],
  templateUrl: './ficha-producto-pagina.html',
  styleUrl: './ficha-producto-pagina.css',
})
export default class FichaProductoPagina implements OnInit {
  private readonly api = inject(CatalogoApiService);
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly avisos = inject(AvisosService);

  protected readonly ficha = signal<FichaProducto | null>(null);
  protected readonly estado = signal<EstadoPantalla>('cargando');
  protected readonly detalleError = signal('');
  private idPostre = 0;

  // RF-2 (Visualización de detalles e ingredientes del producto): inicia aquí. Paso 1: el cliente eligió un producto del catálogo.
  ngOnInit(): void {
    // bind(this): dentro de alCambiarRuta "this" sigue siendo esta página
    this.ruta.paramMap.subscribe(this.alCambiarRuta.bind(this));
  }

  private alCambiarRuta(parametros: ParamMap): void {
    this.idPostre = Number(parametros.get('id'));
    this.cargarFicha();
  }

  // RF-2 · Pasos 2 y 3: pide a la API la ficha del producto.
  async cargarFicha(): Promise<void> {
    // RF-2 · Excepción paso 2: el id no es válido, se trata como producto no disponible.
    if (!Number.isInteger(this.idPostre) || this.idPostre <= 0) {
      this.regresarAlCatalogo('Este producto ya no está disponible');
      return;
    }
    this.estado.set('cargando');
    try {
      this.ficha.set(await this.api.obtenerFicha(this.idPostre));
      // RF-2 · Paso 5: muestra la ficha detallada.
      this.estado.set('listo');
    } catch (error) {
      // RF-2 paso 2: el producto o su categoría ya no están disponibles → regresa al catálogo
      if (tieneCodigo(error, 'PRODUCTO_NO_DISPONIBLE') || tieneCodigo(error, 'DATOS_INVALIDOS')) {
        this.regresarAlCatalogo('Este producto ya no está disponible');
        return;
      }
      // RF-2 · Excepción paso 2: sin conexión, «No fue posible cargar el producto».
      this.detalleError.set(explicarFalla(error));
      this.estado.set('error');
    }
  }

  // RF-2 · Excepción paso 2: deja el aviso «Este producto ya no está disponible» y redirige al catálogo.
  private regresarAlCatalogo(mensaje: string): void {
    this.avisos.dejarAviso(mensaje);
    this.router.navigate(['/']);
  }

  // Texto de un grupo: "Listón, Flores de azúcar ×2"
  textoGrupo(grupo: GrupoIncluido): string {
    const partes: string[] = [];
    for (const opcion of grupo.opciones) {
      if (opcion.cantidad > 1) {
        partes.push(opcion.nombre + ' ×' + opcion.cantidad);
      } else {
        partes.push(opcion.nombre);
      }
    }
    return partes.join(', ');
  }
}
