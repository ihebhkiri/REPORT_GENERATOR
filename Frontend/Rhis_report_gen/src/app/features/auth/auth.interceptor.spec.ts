import {HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {TestBed} from '@angular/core/testing';

import {environment} from '../../../environments/environment';
import {authInterceptor} from './auth.interceptor';
import {AuthService} from './services/auth.service';

describe('Auth refresh interceptor', () => {
  const api = environment.apiBaseUrl || '/api/v1';
  const resource = `${api}/datasets`;
  const refresh = `${api}/auth/refresh`;
  const logout = `${api}/auth/logout`;
  let http: HttpClient;
  let requests: HttpTestingController;
  let auth: AuthService;
  const forbidden = {status: 403, statusText: 'Forbidden'};

  beforeEach(() => {
    TestBed.configureTestingModule({providers: [
      provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting(),
    ]});
    http = TestBed.inject(HttpClient);
    requests = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });
  afterEach(() => requests.verify());

  it('refreshes with cookies and retries the original request once', () => {
    const success = jasmine.createSpy('success');
    http.post(resource, {value: 1}, {withCredentials: true}).subscribe(success);
    requests.expectOne(resource).flush(null, forbidden);
    const token = requests.expectOne(refresh);
    expect(token.request.method).toBe('POST');
    expect(token.request.withCredentials).toBeTrue();
    token.flush(null, {status: 204, statusText: 'No Content'});
    const retry = requests.expectOne(resource);
    expect(retry.request.body).toEqual({value: 1});
    expect(retry.request.withCredentials).toBeTrue();
    retry.flush({ok: true});
    expect(success).toHaveBeenCalledWith({ok: true});
  });

  it('shares a refresh between concurrent failures', () => {
    const success = jasmine.createSpy('success');
    http.get(resource).subscribe(success);
    http.get(`${resource}/2`).subscribe(success);
    requests.expectOne(resource).flush(null, forbidden);
    requests.expectOne(`${resource}/2`).flush(null, forbidden);
    requests.expectOne(refresh).flush(null);
    requests.expectOne(resource).flush([]);
    requests.expectOne(`${resource}/2`).flush([]);
    expect(success).toHaveBeenCalledTimes(2);
  });

  it('propagates a second 403 without another refresh', () => {
    const failure = jasmine.createSpy('failure');
    http.get(resource).subscribe({error: failure});
    requests.expectOne(resource).flush(null, forbidden);
    requests.expectOne(refresh).flush(null);
    requests.expectOne(resource).flush(null, forbidden);
    requests.expectNone(refresh);
    expect(failure.calls.mostRecent().args[0].status).toBe(403);
  });

  it('propagates refresh failure and permits a later fresh attempt', () => {
    for (let attempt = 0; attempt < 2; attempt++) {
      const failure = jasmine.createSpy('failure');
      http.get(resource).subscribe({error: failure});
      requests.expectOne(resource).flush(null, forbidden);
      requests.expectOne(refresh).flush(null, {status: 401, statusText: 'Unauthorized'});
      expect(failure.calls.mostRecent().args[0].status).toBe(401);
      requests.expectNone(resource);
    }
  });

  it('does not refresh auth endpoints or third-party requests', () => {
    for (const url of [refresh, logout, `${api}/auth/login`, 'https://other.test/api/data', '//other.test/data']) {
      const failure = jasmine.createSpy('failure');
      http.post(url, null).subscribe({error: failure});
      requests.expectOne(url).flush(null, forbidden);
      requests.expectNone(refresh);
      expect(failure.calls.mostRecent().args[0].status).toBe(403);
    }
  });

  it('passes through statuses other than 403', () => {
    for (const status of [401, 404, 500]) {
      const failure = jasmine.createSpy('failure');
      http.get(resource).subscribe({error: failure});
      requests.expectOne(resource).flush(null, {status, statusText: 'Error'});
      requests.expectNone(refresh);
      expect(failure.calls.mostRecent().args[0].status).toBe(status);
    }
  });

  it('logs out with cookies and resets loading on failure', () => {
    let error: HttpErrorResponse | undefined;
    auth.logout().subscribe({error: value => error = value});
    expect(auth.loggingOut()).toBeTrue();
    const request = requests.expectOne(logout);
    expect(request.request.method).toBe('POST');
    expect(request.request.withCredentials).toBeTrue();
    request.flush(null, {status: 500, statusText: 'Error'});
    expect(error?.status).toBe(500);
    expect(auth.loggingOut()).toBeFalse();
  });

  it('waits for cookie rotation before logout and suppresses retries during logout', () => {
    http.get(resource).subscribe({error: () => undefined});
    requests.expectOne(resource).flush(null, forbidden);
    auth.logout().subscribe();
    requests.expectNone(logout);
    requests.expectOne(refresh).flush(null);
    requests.expectNone(resource);
    requests.expectOne(logout).flush(null);
    expect(auth.loggingOut()).toBeFalse();
  });

  it('still logs out when an in-flight refresh fails', () => {
    auth.refresh().subscribe({error: () => undefined});
    auth.logout().subscribe();
    requests.expectOne(refresh).flush(null, forbidden);
    requests.expectOne(logout).flush(null);
    expect(auth.loggingOut()).toBeFalse();
  });
});
