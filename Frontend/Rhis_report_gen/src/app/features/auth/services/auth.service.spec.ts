import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../../environments/environment';
import { LoginCredentials } from '../auth.model';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AuthService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('posts login credentials with cookies enabled', () => {
    const credentials: LoginCredentials = {
      email: 'user@example.com',
      password: 'secret',
    };

    service.login(credentials).subscribe();

    const request = httpTesting.expectOne(`${environment.apiBaseUrl}/auth/login`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(credentials);
    expect(request.request.withCredentials).toBeTrue();
    request.flush(null);
  });

  it('loads the current user with cookies enabled', () => {
    service.me().subscribe((user) => expect(user.roles).toEqual(['ROLE_ADMIN']));

    const request = httpTesting.expectOne(`${environment.apiBaseUrl}/auth/me`);
    expect(request.request.method).toBe('GET');
    expect(request.request.withCredentials).toBeTrue();
    request.flush({email: 'admin@example.com', roles: ['ROLE_ADMIN']});
  });
});
