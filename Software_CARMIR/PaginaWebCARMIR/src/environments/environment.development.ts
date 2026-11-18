// Configuración para desarrollo (ng serve).
// La API de ApiCARMIR corre en el puerto 3000 de esta computadora.
export const environment = {
  produccion: false,
  apiUrl: 'http://localhost:3000/api',
  // Las fotos subidas a S3 se guardan como /imagenes/... Para verlas con ng serve, escribe aquí
  // tu dominio de CloudFront (https://dxxxx.cloudfront.net) solo en tu PC; vacío = no se piden a AWS.
  dominioImagenes: '',
};
