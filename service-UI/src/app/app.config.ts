import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from "@angular/platform-browser/animations"
import { providePrimeNG } from "primeng/config"
import Aura from '@primeng/themes/aura';
import { routes } from './app.routes';
import { errorInterceptor } from "./interceptors/error.interceptor"

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([errorInterceptor])),
    provideAnimations(),

    providePrimeNG({
      theme: {
          preset: Aura
      }
  })
  ]
};
