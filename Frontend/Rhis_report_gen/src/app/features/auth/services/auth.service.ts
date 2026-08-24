import {HttpClient} from '@angular/common/http';
import {inject, Injectable} from '@angular/core';
import {Observable} from 'rxjs';

import {environment} from '../../../../environments/environment';
import {CurrentUser, LoginCredentials} from '../auth.model';


@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly loginUrl = `${environment.apiBaseUrl}/auth/login`;
  private readonly meUrl = `${environment.apiBaseUrl}/auth/me`;

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
