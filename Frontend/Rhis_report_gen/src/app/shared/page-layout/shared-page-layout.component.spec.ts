import {TestBed} from '@angular/core/testing';
import {ActivatedRoute, provideRouter} from '@angular/router';
import {Subject} from 'rxjs';
import {AuthService} from '../../features/auth/services/auth.service';
import {CurrentUser} from '../../features/auth/auth.model';
import {SharedPageLayoutComponent} from './shared-page-layout.component';

describe('SharedPageLayoutComponent', () => {
  it('opens and closes the mobile sidebar from the header, backdrop and Escape', async () => {
    await TestBed.configureTestingModule({
      imports: [SharedPageLayoutComponent],
      providers: [
        provideRouter([]),
        {provide: ActivatedRoute, useValue: {snapshot: {data: {page: 'reports'}}}},
        {provide: AuthService, useValue: {me: () => new Subject<CurrentUser>()}},
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(SharedPageLayoutComponent);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const toggle = element.querySelector<HTMLButtonElement>('.mobile-menu-toggle')!;

    expect(toggle.getAttribute('aria-controls')).toBe('shared-sidebar');
    expect(toggle.getAttribute('aria-expanded')).toBe('false');
    toggle.click();
    fixture.detectChanges();
    expect(toggle.getAttribute('aria-expanded')).toBe('true');
    expect(element.querySelector('.shared-sidebar')?.classList).toContain('mobile-open');
    element.querySelector<HTMLButtonElement>('.mobile-sidebar-backdrop')!.click();
    fixture.detectChanges();
    expect(toggle.getAttribute('aria-expanded')).toBe('false');

    toggle.click();
    document.dispatchEvent(new KeyboardEvent('keydown', {key: 'Escape'}));
    fixture.detectChanges();
    expect(toggle.getAttribute('aria-expanded')).toBe('false');
  });

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
      expect(element.querySelectorAll('aside').length).toBe(1);
      expect(element.querySelectorAll('header').length).toBe(1);
      expect(element.querySelectorAll('main').length).toBe(1);
      expect(element.querySelector('a[href="/rapports"]')).not.toBeNull();
      expect(element.querySelector('a[href="/assistant"]')).not.toBeNull();
      expect(element.querySelector('a[href="/administration/datasets"]')).toBeNull();
      response.next({email: 'demo@example.test', roles: ['ROLE_ADMIN']});
      fixture.detectChanges();
      expect(element.querySelector('a[href="/administration/datasets"]')).not.toBeNull();
      const activeHref = page === 'datasets' ? '/administration/datasets'
        : page === 'assistant' ? '/assistant' : '/rapports';
      expect(element.querySelector('.sidebar-nav a[aria-current="page"]')?.getAttribute('href'))
        .toBe(activeHref);
      const toggle = element.querySelector<HTMLButtonElement>('button.sidebar-collapse');
      expect(toggle).not.toBeNull();
      if (!toggle) return;
      const links = Array.from(element.querySelectorAll('.sidebar-nav a'));
      const logo = element.querySelector<HTMLElement>('.sidebar-logo')!;
      const logoImage = logo.querySelector('img')!;
      const chevron = toggle.querySelector('.pi')!;
      expect(logoImage.getAttribute('src')).toBe('/logo_collapsed.png');
      expect(chevron.classList.contains('pi-chevron-right')).toBeTrue();
      expect(getComputedStyle(chevron).transform).toBe('none');
      const collapsedLogoWidth = getComputedStyle(logo).width;
      const shell = element.querySelector<HTMLElement>('.app-shell')!;
      const motionDuration = matchMedia('(prefers-reduced-motion: reduce)').matches ? '0s' : '0.2s';
      for (const animated of [shell, logo]) {
        expect(getComputedStyle(animated).transitionDuration).toBe(motionDuration);
      }
      const collapsedColumns = getComputedStyle(shell).gridTemplateColumns;
      const labels = Array.from(element.querySelectorAll<HTMLElement>('.sidebar-label'));
      expect(labels.length).toBe(9);
      expect(labels.every(label => getComputedStyle(label).display === 'none')).toBeTrue();
      expect(toggle.getAttribute('aria-expanded')).toBe('false');
      expect(toggle.getAttribute('aria-label')).toBe('Développer la navigation');
      expect(toggle.closest('[aria-hidden="true"]')).toBeNull();
      expect(toggle.tabIndex).toBe(0);
      toggle.click();
      fixture.detectChanges();
      await Promise.all(element.getAnimations({subtree: true}).map(animation => animation.finished));
      expect(toggle.getAttribute('aria-expanded')).toBe('true');
      expect(toggle.getAttribute('aria-label')).toBe('Réduire la navigation');
      expect(logoImage.getAttribute('src')).toBe('/rhis-solutions-logo.png');
      expect(chevron.classList.contains('pi-chevron-right')).toBeTrue();
      expect(getComputedStyle(chevron).transform).toBe('none');
      expect(getComputedStyle(toggle).alignSelf).toBe('flex-end');
      expect(labels.every(label => getComputedStyle(label).display !== 'none')).toBeTrue();
      expect(parseFloat(getComputedStyle(logo).width)).toBeGreaterThan(parseFloat(collapsedLogoWidth));
      expect(Array.from(element.querySelectorAll('.sidebar-nav a'))).toEqual(links);
      expect(element.querySelectorAll('aside').length).toBe(1);
      expect(element.querySelector('.sidebar-nav a[aria-current="page"]')?.getAttribute('href'))
        .toBe(activeHref);
      response.next({email: 'demo@example.test', roles: ['ROLE_USER']});
      fixture.detectChanges();
      expect(element.querySelector('a[href="/administration/datasets"]')).toBeNull();
      toggle.click();
      fixture.detectChanges();
      await Promise.all(element.getAnimations({subtree: true}).map(animation => animation.finished));
      expect(toggle.getAttribute('aria-expanded')).toBe('false');
      expect(logoImage.getAttribute('src')).toBe('/logo_collapsed.png');
      expect(getComputedStyle(toggle).alignSelf).toBe('auto');
      expect(getComputedStyle(logo).width).toBe(collapsedLogoWidth);
      expect(getComputedStyle(shell).gridTemplateColumns).toBe(collapsedColumns);
      expect(Array.from(element.querySelectorAll<HTMLElement>('.sidebar-label'))
        .every(label => getComputedStyle(label).display === 'none')).toBeTrue();
      expect(element.querySelector('a[href="/administration/datasets"]')).toBeNull();
      expect(element.querySelectorAll('h1').length).toBe(1);
      if (page === 'configuration') {
        expect(element.querySelector('h1')?.textContent).toContain('Configurer le rapport');
        expect(element.querySelector('.description')?.textContent).toContain(
          'Configurez les champs, les filtres et le tri de votre rapport avant de générer l’aperçu final.',
        );
      }
      response.error(new Error('Session unavailable'));
      fixture.detectChanges();
      expect(element.querySelector('a[href="/administration/datasets"]')).toBeNull();
    });
  }
});
