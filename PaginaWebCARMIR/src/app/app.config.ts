import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { routes } from './app.routes';
import { erroresApiInterceptor } from './core/interceptores/errores-api.interceptor';

// Configuración general de la aplicación
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // Al cambiar de página se regresa al inicio de la pantalla
    provideRouter(routes, withInMemoryScrolling({ scrollPositionRestoration: 'top' })),
    // Peticiones a la API; el interceptor convierte los errores en mensajes { codigo, mensaje }
    provideHttpClient(withFetch(), withInterceptors([erroresApiInterceptor])),
  ],
};
