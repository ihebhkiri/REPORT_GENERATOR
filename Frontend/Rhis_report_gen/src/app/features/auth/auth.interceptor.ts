import {HttpErrorResponse, HttpInterceptorFn} from '@angular/common/http';
import {inject} from '@angular/core';
import {catchError, switchMap, throwError} from 'rxjs';

import {environment} from '../../../environments/environment';
import {AuthService} from './services/auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const apiUrl = environment.apiBaseUrl;
  const isApiRequest = apiUrl ? request.url.startsWith(`${apiUrl}/`) :
    request.url.startsWith('/') && !request.url.startsWith('//');
  if (!isApiRequest ||
      /\/auth\/(login|refresh|logout)(?:\?|$)/.test(request.url)) {
    return next(request);
  }
  const auth = inject(AuthService);
  return next(request).pipe(catchError((error: unknown) => {
    if (!(error instanceof HttpErrorResponse) || error.status !== 401 || auth.loggingOut()) {
      return throwError(() => error);
    }
    return auth.refresh().pipe(switchMap(() =>
      auth.loggingOut() ? throwError(() => error) : next(request),
    ));
  }));
};
