import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

// Arranca la aplicación; si algo falla al iniciar, se muestra en la consola del navegador
bootstrapApplication(App, appConfig).catch(mostrarError);

function mostrarError(error: unknown): void {
  console.error(error);
}
