import {HttpClient} from '@angular/common/http';
import {inject, Injectable, signal} from '@angular/core';
import {catchError, defer, finalize, Observable, of, shareReplay, switchMap, throwError} from 'rxjs';

import {environment} from '../../../../environments/environment';
import {CurrentUser, LoginCredentials} from '../auth.model';


@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly loginUrl = `${environment.apiBaseUrl}/auth/login`;
  private readonly meUrl = `${environment.apiBaseUrl}/auth/me`;
  private readonly authUrl = `${environment.apiBaseUrl || '/api/v1'}/auth`;
  private refreshRequest: Observable<void> | undefined;
  readonly loggingOut = signal(false);

  refresh(): Observable<void> {
    if (this.loggingOut()) {
      return throwError(() => new Error('Déconnexion en cours'));
    }
    this.refreshRequest ??= this.http.post<void>(`${this.authUrl}/refresh`, null, {
      withCredentials: true,
    }).pipe(
      finalize(() => this.refreshRequest = undefined),
      shareReplay({bufferSize: 1, refCount: false}),
    );
    return this.refreshRequest;
  }

  logout(): Observable<void> {
    return defer(() => {
      this.loggingOut.set(true);
      // Finish cookie rotation before clearing the session cookies.
      return (this.refreshRequest ?? of(undefined)).pipe(
        catchError(() => of(undefined)),
        switchMap(() => this.http.post<void>(`${this.authUrl}/logout`, null, {
          withCredentials: true,
        })),
        finalize(() => this.loggingOut.set(false)),
      );
    });
  }

  login(credentials: LoginCredentials): Observable<void> {
    return this.http.post<void>(this.loginUrl, credentials, {
      withCredentials: true,
    });
  }

  me(): Observable<CurrentUser> {
    return this.http.get<CurrentUser>(this.meUrl, {
      withCredentials: true,
    });
  }
}
