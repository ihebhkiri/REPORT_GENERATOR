import {TestBed} from '@angular/core/testing';
import {CanActivateFn, provideRouter, Router, UrlTree} from '@angular/router';
import {firstValueFrom, Observable, of} from 'rxjs';

import {AuthService} from '../services/auth.service';
import {adminGuard} from './admin.guard';

describe('adminGuard', () => {
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['me']);
    TestBed.configureTestingModule({
      providers: [provideRouter([]), {provide: AuthService, useValue: authService}],
    });
  });

  it('allows an administrator', async () => {
    authService.me.and.returnValue(of({email: 'admin@example.com', roles: ['ROLE_ADMIN']}));

    expect(await runGuard()).toBeTrue();
  });

  it('redirects a non administrator to reports', async () => {
    authService.me.and.returnValue(of({email: 'reader@example.com', roles: ['ROLE_USER']}));

    const result = await runGuard();
    expect(result instanceof UrlTree).toBeTrue();
    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/rapports');
  });

  function runGuard(): Promise<boolean | UrlTree> {
    const result = TestBed.runInInjectionContext(() =>
      adminGuard({} as Parameters<CanActivateFn>[0], {} as Parameters<CanActivateFn>[1]),
    );
    return firstValueFrom(result as Observable<boolean | UrlTree>);
  }
});
