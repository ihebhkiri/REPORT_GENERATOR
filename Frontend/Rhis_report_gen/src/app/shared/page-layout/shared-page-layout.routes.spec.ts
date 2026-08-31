import {provideHttpClient} from '@angular/common/http';
import {provideHttpClientTesting} from '@angular/common/http/testing';
import {TestBed} from '@angular/core/testing';
import {By} from '@angular/platform-browser';
import {provideNoopAnimations} from '@angular/platform-browser/animations';
import {provideRouter, Router} from '@angular/router';
import {RouterTestingHarness} from '@angular/router/testing';
import {MessageService} from 'primeng/api';
import {of} from 'rxjs';
import {routes} from '../../app.routes';
import {AuthService} from '../../features/auth/services/auth.service';
import {DatasetExposureComponent} from '../../features/administration/dataset-exposure/dataset-exposure.component';
import {DatasetExposureService} from '../../features/administration/dataset-exposure/dataset-exposure.service';
import {DatasetService} from '../../features/rapports/services/dataset.service';

describe('Shared page layout routes', () => {
  let roles: string[];
  beforeEach(() => {
    roles = ['ROLE_ADMIN'];
    TestBed.configureTestingModule({providers: [
      provideRouter(routes), provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations(),
      MessageService,
      {provide: AuthService, useValue: {me: () => of({email: 'test@example.test', roles})}},
      {provide: DatasetService, useValue: {getReportSources: () => of({datasets: [], relations: []})}},
      {provide: DatasetExposureService, useValue: {getConfiguration: () => of({datasets: [{
        id: 1, displayName: 'Employés', active: true, displayMain: true, displayRelated: false,
        visibleFieldCount: 0, fields: [],
      }]})}},
    ]});
  });

  it('preserves the dirty guard when navigating through the shared header', async () => {
    const harness = await RouterTestingHarness.create('/administration/datasets');
    const page = harness.fixture.debugElement.query(By.directive(DatasetExposureComponent))
      .componentInstance as DatasetExposureComponent;
    page.updateMode(1, 'NONE');
    harness.detectChanges();
    const confirmation = spyOn(window, 'confirm').and.returnValue(false);
    const reportLink = harness.routeNativeElement?.querySelector('a[href="/rapports"]') as HTMLAnchorElement;
    reportLink.click();
    await harness.fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/administration/datasets');
    expect(page.dirty()).toBeTrue();
    expect(confirmation).toHaveBeenCalled();
    confirmation.and.returnValue(true);
    await harness.navigateByUrl('/rapports');
    expect(harness.routeNativeElement?.querySelectorAll('.shared-header').length).toBe(1);
    expect(harness.routeNativeElement?.querySelectorAll('h1').length).toBe(1);
    expect(harness.routeNativeElement?.textContent).toContain('Créer un rapport dynamique');
  });

  it('redirects a non-admin and hides the data link after navigation', async () => {
    roles = ['ROLE_USER'];
    const harness = await RouterTestingHarness.create('/administration/datasets');
    expect(TestBed.inject(Router).url).toBe('/rapports');
    expect(harness.routeNativeElement?.querySelector('a[href="/administration/datasets"]')).toBeNull();
    expect(harness.routeNativeElement?.querySelector('a[href="/rapports"]')).not.toBeNull();
  });

  it('does not add the shared header to login and uses it once on report configuration', async () => {
    const harness = await RouterTestingHarness.create('/login');
    expect(harness.routeNativeElement?.querySelector('.shared-header')).toBeNull();
    await harness.navigateByUrl('/rapports/configuration/invalid');
    expect(harness.routeNativeElement?.querySelectorAll('.shared-header').length).toBe(1);
    expect(harness.routeNativeElement?.querySelectorAll('h1').length).toBe(1);
    expect(TestBed.inject(Router).url).toBe('/rapports/configuration/invalid');
  });

  it('uses one shared header and title on report export', async () => {
    const harness = await RouterTestingHarness.create('/rapports/export/generation-1');
    expect(harness.routeNativeElement?.querySelectorAll('.shared-header').length).toBe(1);
    expect(harness.routeNativeElement?.querySelectorAll('h1').length).toBe(1);
    expect(TestBed.inject(Router).url).toBe('/rapports/export/generation-1');
  });

  it('loads the report assistant inside the shared layout', async () => {
    const harness = await RouterTestingHarness.create('/assistant');
    expect(TestBed.inject(Router).url).toBe('/assistant');
    expect(harness.routeNativeElement?.querySelectorAll('.shared-header').length).toBe(1);
    expect(harness.routeNativeElement?.querySelector('app-report-assistant')).not.toBeNull();
    expect(harness.routeNativeElement?.textContent).toContain('Assistant de rapports');
  });
});
