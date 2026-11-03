/**
 * Formas del pastel 3D (contornos en 2D que luego se "estiran" hacia arriba).
 * Aquí no se usa three.js: solo se calculan puntos, así sirven para el modelo 3D
 * y para los iconos SVG de los botones de forma.
 */

export type FormaPastel = 'redondo' | 'cuadrado' | 'corazon' | 'hexagono' | 'estrella' | 'flor' | 'ovalado';

export interface Punto2d {
  x: number;
  y: number;
}

export const FORMAS_PASTEL: FormaPastel[] = ['redondo', 'cuadrado', 'corazon', 'hexagono', 'estrella', 'flor', 'ovalado'];

// Radio (de 0 a 1) de cada forma en el ángulo "t" (en radianes)
function radioEn(forma: FormaPastel, t: number): number {
  if (forma === 'cuadrado') {
    // Cuadrado con esquinas redondeadas (superelipse)
    const potencia = 7;
    const c = Math.pow(Math.abs(Math.cos(t)), potencia);
    const s = Math.pow(Math.abs(Math.sin(t)), potencia);
    return Math.pow(c + s, -1 / potencia);
  }
  if (forma === 'hexagono') {
    const lado = Math.PI / 3;
    let resto = t % lado;
    if (resto < 0) {
      resto = resto + lado;
    }
    return Math.cos(Math.PI / 6) / Math.cos(resto - Math.PI / 6);
  }
  if (forma === 'estrella') {
    // Cinco picos suaves
    const onda = Math.cos(5 * (t - Math.PI / 2));
    return 0.68 + 0.32 * Math.pow((onda + 1) / 2, 1.6);
  }
  if (forma === 'flor') {
    return 1 + 0.11 * Math.cos(6 * t);
  }
  return 1;
}

// Corazón: curva clásica en coordenadas x, y
function puntoCorazon(t: number): Punto2d {
  const x = 16 * Math.pow(Math.sin(t), 3);
  const y = 13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t);
  return { x: x, y: y };
}

// Centra los puntos y los ajusta para que el lado más largo mida "tamano"
function ajustar(puntos: Punto2d[], tamano: number): Punto2d[] {
  let minX = Infinity;
  let minY = Infinity;
  let maxX = -Infinity;
  let maxY = -Infinity;
  for (const punto of puntos) {
    minX = Math.min(minX, punto.x);
    maxX = Math.max(maxX, punto.x);
    minY = Math.min(minY, punto.y);
    maxY = Math.max(maxY, punto.y);
  }
  const escala = tamano / Math.max(maxX - minX, maxY - minY);
  const centroX = (minX + maxX) / 2;
  const centroY = (minY + maxY) / 2;
  const ajustados: Punto2d[] = [];
  for (const punto of puntos) {
    ajustados.push({ x: (punto.x - centroX) * escala, y: (punto.y - centroY) * escala });
  }
  return ajustados;
}

/**
 * Contorno de la forma con "cantidad" puntos, en sentido contrario a las manecillas del reloj.
 * "tamano" es el ancho mayor (diámetro) en centímetros.
 */
export function puntosForma(forma: FormaPastel, tamano: number, cantidad: number): Punto2d[] {
  const puntos: Punto2d[] = [];
  for (let i = 0; i < cantidad; i++) {
    const t = (i / cantidad) * Math.PI * 2;
    if (forma === 'corazon') {
      // El corazón se dibuja en sentido de las manecillas; se recorre al revés
      puntos.push(puntoCorazon(Math.PI * 2 - t));
    } else if (forma === 'ovalado') {
      puntos.push({ x: Math.cos(t), y: Math.sin(t) * 0.72 });
    } else {
      const radio = radioEn(forma, t);
      puntos.push({ x: Math.cos(t) * radio, y: Math.sin(t) * radio });
    }
  }
  return ajustar(puntos, tamano);
}

// Ruta SVG de la forma para los iconos (caja cuadrada de "caja" pixeles)
export function rutaSvgForma(forma: FormaPastel, caja: number): string {
  const puntos = puntosForma(forma, caja * 0.84, 72);
  let ruta = '';
  for (let i = 0; i < puntos.length; i++) {
    let letra = 'L';
    if (i === 0) {
      letra = 'M';
    }
    const x = puntos[i].x + caja / 2;
    const y = caja / 2 - puntos[i].y;
    ruta = ruta + letra + x.toFixed(1) + ' ' + y.toFixed(1) + ' ';
  }
  return ruta + 'Z';
}

// ¿El punto (x, y) está dentro del contorno?
export function estaDentro(x: number, y: number, contorno: Punto2d[]): boolean {
  let dentro = false;
  let j = contorno.length - 1;
  for (let i = 0; i < contorno.length; i++) {
    const a = contorno[i];
    const b = contorno[j];
    const cruza = a.y > y !== b.y > y;
    if (cruza && x < ((b.x - a.x) * (y - a.y)) / (b.y - a.y) + a.x) {
      dentro = !dentro;
    }
    j = i;
  }
  return dentro;
}
