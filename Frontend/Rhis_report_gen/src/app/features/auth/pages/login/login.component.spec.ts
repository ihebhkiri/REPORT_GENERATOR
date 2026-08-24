import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';

import { AuthService } from '../../services/auth.service';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;
  let router: Router;

  const authService = {
    login: jasmine.createSpy().and.returnValue(of(void 0)),
  };

  beforeEach(async () => {
    authService.login.calls.reset();
    authService.login.and.returnValue(of(void 0));

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [provideRouter([]), { provide: AuthService, useValue: authService }],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('marks an invalid form as touched without submitting it', () => {
    component.submit();

    expect(component.email.touched).toBeTrue();
    expect(component.password.touched).toBeTrue();
    expect(authService.login).not.toHaveBeenCalled();
  });

  it('keeps loading active and ignores duplicate submissions until login completes', () => {
    const loginRequest = new Subject<void>();
    const navigateSpy = spyOn(router, 'navigateByUrl').and.resolveTo(true);
    authService.login.and.returnValue(loginRequest);
    component.loginForm.setValue({ email: 'user@example.com', password: 'secret' });

    component.submit();

    expect(component.isLoading()).toBeTrue();
    expect(authService.login).toHaveBeenCalledOnceWith({
      email: 'user@example.com',
      password: 'secret',
    });

    component.submit();
    expect(authService.login).toHaveBeenCalledTimes(1);

    loginRequest.next();
    loginRequest.complete();

    expect(navigateSpy).toHaveBeenCalledOnceWith('/rapports');
    expect(component.isLoading()).toBeFalse();
  });

  it('shows an error and stops loading when login fails', () => {
    authService.login.and.returnValue(throwError(() => new Error('Unauthorized')));
    component.loginForm.setValue({ email: 'user@example.com', password: 'wrong' });

    component.submit();

    expect(component.errorMessage()).toBe('Vérifiez vos identifiants');
    expect(component.isLoading()).toBeFalse();
  });
});
