import {
  bytesABase64,
  calcularAnticipo,
  textoTamano,
  tipoDeArchivo,
  validarArchivoComprobante,
  contactoConsulta,
  validarCodigoPostal,
  validarContactoConsulta,
  validarCorreo,
  validarEnlace,
  validarFolio,
  validarMontoComprobante,
  validarNombre,
  validarTelefono,
  validarTextoCampo,
} from './validacion-pedido';

describe('validaciones del Portal de Pedidos', function () {
  it('datos de contacto', function () {
    expect(validarNombre('Ana López')).toBe('');
    expect(validarNombre('A1')).not.toBe('');
    expect(validarTelefono('4421234567')).toBe('');
    expect(validarTelefono('442 123 4567')).not.toBe('');
    expect(validarCorreo('')).toBe('');
    expect(validarCorreo('ana@correo')).not.toBe('');
  });

  it('consulta con teléfono o correo', function () {
    expect(contactoConsulta('442 123-4567')).toEqual({ telefono: '4421234567', correo: '' });
    expect(contactoConsulta(' ana@correo.com ')).toEqual({ telefono: '', correo: 'ana@correo.com' });
    expect(validarContactoConsulta('4421234567')).toBe('');
    expect(validarContactoConsulta('ana@correo.com')).toBe('');
    expect(validarContactoConsulta('')).not.toBe('');
    expect(validarContactoConsulta('12345')).not.toBe('');
    expect(validarContactoConsulta('ana@correo')).not.toBe('');
  });

  it('dirección y textos libres', function () {
    expect(validarCodigoPostal('76000')).toBe('');
    expect(validarCodigoPostal('7600')).not.toBe('');
    expect(validarTextoCampo('', 'la calle', 100, true)).toBe('Indica la calle');
    expect(validarTextoCampo('', 'referencias', 255, false)).toBe('');
    expect(validarTextoCampo('Casa <b>', 'referencias', 255, false)).not.toBe('');
  });

  it('folio, comprobante y anticipo', function () {
    expect(validarFolio('#25')).toBe('');
    expect(validarFolio('abc')).not.toBe('');
    expect(validarMontoComprobante(100, 100)).toBe('');
    expect(validarMontoComprobante(101, 100)).not.toBe('');
    expect(validarEnlace('https://fotos.com/a.jpg')).toBe('');
    expect(validarEnlace('javascript:alert(1)')).not.toBe('');
    expect(calcularAnticipo(891.95, 50, 'Anticipo')).toBe(445.98);
    expect(calcularAnticipo(891.95, 50, 'Pago total')).toBe(891.95);
  });

  it('archivo del comprobante: tipo y tamaño', function () {
    expect(validarArchivoComprobante('image/png', 1000)).toBe('');
    expect(validarArchivoComprobante('application/pdf', 5 * 1024 * 1024)).toBe('');
    expect(validarArchivoComprobante('image/gif', 1000)).toContain('JPG, PNG o WEBP');
    expect(validarArchivoComprobante('image/jpeg', 5 * 1024 * 1024 + 1)).toContain('5 MB');
    expect(validarArchivoComprobante('image/jpeg', 0)).not.toBe('');
    expect(tipoDeArchivo('recibo.JPG', '')).toBe('image/jpeg');
    expect(tipoDeArchivo('recibo.pdf', 'application/pdf')).toBe('application/pdf');
    expect(tipoDeArchivo('recibo.heic', '')).toBe('');
    expect(textoTamano(1536 * 1024)).toBe('1.5 MB');
    expect(textoTamano(2048)).toBe('2 KB');
    expect(bytesABase64(new Uint8Array([37, 80, 68, 70]))).toBe('JVBERg==');
  });
});
