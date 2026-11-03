/*
 * Reglas de la entrega (RF-9) y del pago (RF-10) que vienen de la tabla Configuracion.
 * Son los mismos parámetros que edita la Administradora en el apartado "Configuración" del Panel.
 *
 *  - DiasAnticipacionMinima: días mínimos entre hoy y la entrega.
 *  - DiasLaborables:         días con entrega (L M X J V S D separados por coma).
 *  - HoraApertura / HoraCierre: primer y último horario de entrega.
 *  - CapacidadDiaria:        pedidos por día; un día lleno ya no se ofrece (agenda del Panel).
 *  - PorcentajeAnticipo:     anticipo mínimo para la modalidad "Anticipo".
 *  - DatosBancarios:         si está vacío, solo se acepta pago en sucursal.
 *
 * Las fechas se manejan como texto "AAAA-MM-DD" en la hora local de la computadora
 * (la misma que usa MySQL y el Panel), así no se recorren por la zona horaria.
 */

// Hasta cuántos días adelante se aceptan pedidos
const DIAS_MAXIMOS_ADELANTE = 90;
// Cada cuánto se ofrecen horarios (minutos)
const MINUTOS_ENTRE_HORARIOS = 30;

// Letra de cada día según Date.getDay() (0 = domingo)
const LETRAS_DIAS = ['D', 'L', 'M', 'X', 'J', 'V', 'S'];
const NOMBRES_DIAS = ['domingo', 'lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado'];

// Valores de fábrica (los mismos de MIGRACION_COMPLETA.sql) por si falta algún parámetro
const POR_DEFECTO = {
  NombreNegocio: 'Delicias de Postres',
  CapacidadDiaria: '6',
  DiasAnticipacionMinima: '3',
  HoraApertura: '09:00',
  HoraCierre: '19:00',
  DiasLaborables: 'L,M,X,J,V,S',
  PorcentajeAnticipo: '50',
  DatosBancarios: ''
};

// ===================== Lectura de parámetros =====================

// Convierte las filas [{ Clave, Valor }] en las reglas que usa el portal
function armarReglas(filas) {
  const valores = {};
  for (let i = 0; i < filas.length; i++) {
    valores[filas[i].Clave] = String(filas[i].Valor).trim();
  }

  const apertura = horaValida(leer(valores, 'HoraApertura'), POR_DEFECTO.HoraApertura);
  let cierre = horaValida(leer(valores, 'HoraCierre'), POR_DEFECTO.HoraCierre);
  if (aMinutos(cierre) < aMinutos(apertura)) {
    cierre = apertura;
  }

  const porcentaje = enteroEnRango(leer(valores, 'PorcentajeAnticipo'), 0, 100, 50);
  let diasLaborables = leerDias(leer(valores, 'DiasLaborables'));
  if (diasLaborables.length === 0) {
    diasLaborables = leerDias(POR_DEFECTO.DiasLaborables);
  }

  return {
    nombreNegocio: leer(valores, 'NombreNegocio'),
    capacidadDiaria: enteroEnRango(leer(valores, 'CapacidadDiaria'), 1, 1000, 6),
    diasAnticipacionMinima: enteroEnRango(leer(valores, 'DiasAnticipacionMinima'), 0, 365, 3),
    diasMaximosAdelante: DIAS_MAXIMOS_ADELANTE,
    horaApertura: apertura,
    horaCierre: cierre,
    diasLaborables: diasLaborables,
    porcentajeAnticipo: porcentaje,
    datosBancarios: leer(valores, 'DatosBancarios')
  };
}

function leer(valores, clave) {
  const valor = valores[clave];
  if (valor === undefined || valor === null) {
    return POR_DEFECTO[clave];
  }
  return valor;
}

function enteroEnRango(texto, minimo, maximo, porDefecto) {
  if (!/^[0-9]{1,4}$/.test(texto)) {
    return porDefecto;
  }
  const numero = Number(texto);
  if (numero < minimo || numero > maximo) {
    return porDefecto;
  }
  return numero;
}

// "L,M,X" → ['L', 'M', 'X'] (solo letras válidas, sin repetir)
function leerDias(texto) {
  const dias = [];
  const partes = String(texto).toUpperCase().split(',');
  for (let i = 0; i < partes.length; i++) {
    const letra = partes[i].trim();
    if (LETRAS_DIAS.indexOf(letra) >= 0 && dias.indexOf(letra) < 0) {
      dias.push(letra);
    }
  }
  return dias;
}

// ===================== Horas =====================

// "9:00" o "09:00" → "09:00"; si no es válida regresa el valor por defecto
function horaValida(texto, porDefecto) {
  const partes = /^([0-9]{1,2}):([0-9]{2})$/.exec(String(texto));
  if (partes === null) {
    return porDefecto;
  }
  const horas = Number(partes[1]);
  const minutos = Number(partes[2]);
  if (horas > 23 || minutos > 59) {
    return porDefecto;
  }
  return dosDigitos(horas) + ':' + dosDigitos(minutos);
}

function aMinutos(hora) {
  const partes = hora.split(':');
  return Number(partes[0]) * 60 + Number(partes[1]);
}

function deMinutos(totalMinutos) {
  return dosDigitos(Math.floor(totalMinutos / 60)) + ':' + dosDigitos(totalMinutos % 60);
}

function dosDigitos(numero) {
  if (numero < 10) {
    return '0' + numero;
  }
  return String(numero);
}

// Horarios que se ofrecen: de la apertura al cierre cada 30 minutos
// RF-9 · Paso 6: horarios de atención que se ofrecen.
function horariosDisponibles(reglas) {
  const horarios = [];
  const inicio = aMinutos(reglas.horaApertura);
  const fin = aMinutos(reglas.horaCierre);
  let actual = inicio;
  while (actual <= fin) {
    horarios.push(deMinutos(actual));
    actual = actual + MINUTOS_ENTRE_HORARIOS;
  }
  return horarios;
}

// ===================== Fechas =====================

// Fecha local de hoy como "AAAA-MM-DD"
function hoy() {
  return fechaATexto(new Date());
}

function fechaATexto(fecha) {
  return fecha.getFullYear() + '-' + dosDigitos(fecha.getMonth() + 1) + '-' + dosDigitos(fecha.getDate());
}

// "AAAA-MM-DD" → Date (a mediodía local, para que el cambio de horario no mueva el día); null si no existe
function textoAFecha(texto) {
  const partes = /^([0-9]{4})-([0-9]{2})-([0-9]{2})$/.exec(String(texto));
  if (partes === null) {
    return null;
  }
  const anio = Number(partes[1]);
  const mes = Number(partes[2]);
  const dia = Number(partes[3]);
  const fecha = new Date(anio, mes - 1, dia, 12, 0, 0);
  // new Date(2026, 1, 31) se convierte en marzo: así se detectan fechas que no existen
  if (fecha.getFullYear() !== anio || fecha.getMonth() !== mes - 1 || fecha.getDate() !== dia) {
    return null;
  }
  return fecha;
}

function sumarDias(texto, dias) {
  const fecha = textoAFecha(texto);
  fecha.setDate(fecha.getDate() + dias);
  return fechaATexto(fecha);
}

// "2026-11-10" → "10/11/2026" (como se muestra al cliente)
function fechaParaMostrar(texto) {
  const partes = String(texto).substring(0, 10).split('-');
  if (partes.length !== 3) {
    return String(texto);
  }
  return partes[2] + '/' + partes[1] + '/' + partes[0];
}

function esDiaLaborable(reglas, texto) {
  const fecha = textoAFecha(texto);
  if (fecha === null) {
    return false;
  }
  return reglas.diasLaborables.indexOf(LETRAS_DIAS[fecha.getDay()]) >= 0;
}

// Primer día que cumple la anticipación mínima y es laborable
// RF-9 · Paso 6: primera fecha que cumple la anticipación mínima y es laborable.
function primeraFechaValida(reglas) {
  let fecha = sumarDias(hoy(), reglas.diasAnticipacionMinima);
  for (let i = 0; i < 7; i++) {
    if (esDiaLaborable(reglas, fecha)) {
      return fecha;
    }
    fecha = sumarDias(fecha, 1);
  }
  return fecha;
}

// "lunes a sábado" o la lista de días ("lunes, miércoles y viernes")
function textoDiasLaborables(reglas) {
  const nombres = [];
  // Se recorre de lunes (1) a domingo (0) para que la lista salga en orden
  const orden = [1, 2, 3, 4, 5, 6, 0];
  for (let i = 0; i < orden.length; i++) {
    if (reglas.diasLaborables.indexOf(LETRAS_DIAS[orden[i]]) >= 0) {
      nombres.push(NOMBRES_DIAS[orden[i]]);
    }
  }
  if (nombres.length === 7) {
    return 'todos los días';
  }
  // Días seguidos (por ejemplo de lunes a sábado): "lunes a sábado"
  if (nombres.length >= 3 && sonDiasSeguidos(reglas, orden)) {
    return nombres[0] + ' a ' + nombres[nombres.length - 1];
  }
  if (nombres.length === 1) {
    return nombres[0];
  }
  return nombres.slice(0, nombres.length - 1).join(', ') + ' y ' + nombres[nombres.length - 1];
}

// true si los días laborables no tienen huecos en la semana (de lunes a domingo)
function sonDiasSeguidos(reglas, orden) {
  let primero = -1;
  let ultimo = -1;
  let cuantos = 0;
  for (let i = 0; i < orden.length; i++) {
    if (reglas.diasLaborables.indexOf(LETRAS_DIAS[orden[i]]) >= 0) {
      if (primero < 0) {
        primero = i;
      }
      ultimo = i;
      cuantos = cuantos + 1;
    }
  }
  return ultimo - primero + 1 === cuantos;
}

/*
 * Revisa la fecha y el horario elegidos (RF-9 paso 6).
 * cargaDelDia = pedidos que ya hay ese día. Regresa { campo, mensaje } o null si todo está bien.
 */
// RF-9 · Paso 6 (API): valida la anticipación de la fecha, el día laborable, el cupo y el horario.
function revisarFechaYHorario(reglas, fecha, horario, cargaDelDia) {
  const minima = primeraFechaValida(reglas);
  const mensajeFecha = 'Elige una fecha a partir del ' + fechaParaMostrar(minima) +
    ' (entregamos ' + textoDiasLaborables(reglas) + ')';

  // RF-9 · Excepción paso 6: la fecha no existe.
  if (textoAFecha(fecha) === null) {
    return { campo: 'fecha', mensaje: 'Indica una fecha de entrega válida' };
  }
  // RF-9 · Excepción paso 6: «Elige una fecha a partir del…» (sin anticipación o día no laborable).
  if (fecha < sumarDias(hoy(), reglas.diasAnticipacionMinima) || !esDiaLaborable(reglas, fecha)) {
    return { campo: 'fecha', mensaje: mensajeFecha };
  }
  // RF-9 · Excepción paso 6: fecha demasiado lejana.
  if (fecha > sumarDias(hoy(), reglas.diasMaximosAdelante)) {
    return {
      campo: 'fecha',
      mensaje: 'Solo recibimos pedidos con hasta ' + reglas.diasMaximosAdelante + ' días de anticipación'
    };
  }
  // RF-9 · Excepción paso 6: el día ya no tiene cupo.
  if (cargaDelDia >= reglas.capacidadDiaria) {
    return {
      campo: 'fecha',
      mensaje: 'Ya no tenemos cupo para el ' + fechaParaMostrar(fecha) + '. Elige otra fecha'
    };
  }
  // RF-9 · Excepción paso 6: horario fuera del horario de atención (se muestran los disponibles).
  if (horariosDisponibles(reglas).indexOf(horario) < 0) {
    return {
      campo: 'horario',
      mensaje: 'Elige un horario entre las ' + reglas.horaApertura + ' y las ' + reglas.horaCierre
    };
  }
  return null;
}

// ===================== Pago =====================

// Transferencia solo si la Administradora capturó sus datos bancarios
// RF-10 · Excepción paso 6: sin datos bancarios no se acepta transferencia.
function aceptaTransferencia(reglas) {
  return reglas.datosBancarios !== '';
}

// Anticipo requerido según la modalidad (con 2 decimales)
// RF-10 · Paso 3: anticipo mínimo según la modalidad.
function calcularAnticipo(reglas, total, modalidadPago) {
  if (modalidadPago === 'Pago total') {
    return total;
  }
  return Math.round(total * reglas.porcentajeAnticipo) / 100;
}

module.exports = {
  armarReglas: armarReglas,
  horariosDisponibles: horariosDisponibles,
  hoy: hoy,
  sumarDias: sumarDias,
  textoAFecha: textoAFecha,
  fechaParaMostrar: fechaParaMostrar,
  esDiaLaborable: esDiaLaborable,
  primeraFechaValida: primeraFechaValida,
  textoDiasLaborables: textoDiasLaborables,
  revisarFechaYHorario: revisarFechaYHorario,
  aceptaTransferencia: aceptaTransferencia,
  calcularAnticipo: calcularAnticipo
};
