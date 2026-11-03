import { CurrencyPipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ImagenPostre } from '../imagen-postre/imagen-postre';

// Datos mínimos que necesita la tarjeta (los cumple ProductoCatalogo)
export interface DatosTarjeta {
  idPostre: number;
  nombre: string;
  descripcion: string | null;
  linkFotografia: string | null;
  precio: number;
  nombreCategoria: string;
}

/**
 * Tarjeta de un producto en la cuadrícula del catálogo (diseño catalogo.html).
 */
@Component({
  selector: 'app-tarjeta-producto',
  imports: [RouterLink, CurrencyPipe, ImagenPostre],
  templateUrl: './tarjeta-producto.html',
  styleUrl: './tarjeta-producto.css',
})
export class TarjetaProducto {
  readonly producto = input.required<DatosTarjeta>();
}
