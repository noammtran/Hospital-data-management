import { HttpErrorResponse, HttpHandlerFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { TokenService } from '@core/authentication';
import { catchError, throwError } from 'rxjs';
import { BASE_URL, hasHttpScheme } from './base-url-interceptor';

export function tokenInterceptor(req: HttpRequest<unknown>, next: HttpHandlerFn) {
  const baseUrl = inject(BASE_URL, { optional: true });
  const tokenService = inject(TokenService);

  const includeBaseUrl = (url: string) => {
    if (!baseUrl) {
      return false;
    }
    const root = baseUrl.replace(/\/$/, '').toLowerCase();
    const target = url.toLowerCase();
    return target === root || target.startsWith(root + '/');
  };

  const isApiRequest = (url: string) => /(^|\/)api\/v1(?:\/|$)/i.test(url);
  const isLoginRequest = (url: string) => /(^|\/)api\/v1\/auth\/login(?:\?|$)/i.test(url);
  const shouldAppendToken = (url: string) =>
    isApiRequest(url) && !isLoginRequest(url) && (!hasHttpScheme(url) || includeBaseUrl(url));

  if (tokenService.valid() && shouldAppendToken(req.url)) {
    return next(
      req.clone({
        headers: req.headers.append('Authorization', tokenService.getBearerToken()),
      })
    ).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          tokenService.clear();
        }
        return throwError(() => error);
      })
    );
  }

  return next(req);
}
