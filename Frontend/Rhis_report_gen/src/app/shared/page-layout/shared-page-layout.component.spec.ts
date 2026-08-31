import {TestBed} from '@angular/core/testing';
import {ActivatedRoute, provideRouter} from '@angular/router';
import {Subject} from 'rxjs';
import {AuthService} from '../../features/auth/services/auth.service';
import {CurrentUser} from '../../features/auth/auth.model';
import {SharedPageLayoutComponent} from './shared-page-layout.component';

describe('SharedPageLayoutComponent', () => {
  for (const page of ['reports', 'datasets', 'assistant', 'configuration', 'export']) {
    it('renders one header and role-aware navigation on ' + page, async () => {
      const response = new Subject<CurrentUser>();
      await TestBed.configureTestingModule({
        imports: [SharedPageLayoutComponent],
        providers: [
          provideRouter([]),
          {provide: ActivatedRoute, useValue: {snapshot: {data: {page}}}},
          {provide: AuthService, useValue: {me: () => response}},
        ],
      }).compileComponents();
      const fixture = TestBed.createComponent(SharedPageLayoutComponent);
      fixture.detectChanges();
      const element = fixture.nativeElement as HTMLElement;
      expect(element.querySelectorAll('header').length).toBe(1);
      expect(element.querySelector('a[href="/rapports"]')).not.toBeNull();
      expect(element.querySelector('a[href="/assistant"]')).not.toBeNull();
      expect(element.querySelector('a[href="/administration/datasets"]')).toBeNull();
      response.next({email: 'demo@example.test', roles: ['ROLE_ADMIN']});
      fixture.detectChanges();
      expect(element.querySelector('a[href="/administration/datasets"]')).not.toBeNull();
      expect(element.querySelector('a[aria-current="page"]')?.textContent)
        .toContain(page === 'datasets' ? 'Données' : page === 'assistant' ? 'Assistant' : 'Rapports');
      response.next({email: 'demo@example.test', roles: ['ROLE_USER']});
      fixture.detectChanges();
      expect(element.querySelector('a[href="/administration/datasets"]')).toBeNull();
      expect(element.querySelectorAll('h1').length).toBe(1);
      response.error(new Error('Session unavailable'));
      fixture.detectChanges();
      expect(element.querySelector('a[href="/administration/datasets"]')).toBeNull();
    });
  }
});
