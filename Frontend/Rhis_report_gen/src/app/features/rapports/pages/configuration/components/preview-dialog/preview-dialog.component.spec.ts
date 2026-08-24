import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { ReportPreviewResponse } from '../../../../models/report-preview.model';
import { PreviewDialogComponent } from './preview-dialog.component';

describe('PreviewDialogComponent', () => {
  let fixture: ComponentFixture<PreviewDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PreviewDialogComponent],
      providers: [provideNoopAnimations()],
    }).compileComponents();

    fixture = TestBed.createComponent(PreviewDialogComponent);
    fixture.componentRef.setInput('visible', true);
  });

  it('renders the six rows returned by the API in column order', () => {
    const response: ReportPreviewResponse = {
      columns: [
        { key: 'field_2', fieldId: 2, displayName: 'Nom', dataType: 'TEXT' },
        { key: 'field_1', fieldId: 1, displayName: 'Actif', dataType: 'BOOLEAN' },
      ],
      rows: Array.from({ length: 6 }, (_, index) => ({
        field_1: index % 2 === 0,
        field_2: `Employé ${index + 1}`,
      })),
      hasMore: true,
      returnedRowCount: 6,
    };
    fixture.componentRef.setInput('response', response);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelectorAll('tbody tr').length).toBe(6);
    expect(element.querySelector('thead')?.textContent).toContain('Nom');
    expect(element.querySelector('thead')?.textContent).toContain('Actif');
    expect(element.textContent).toContain('Oui');
  });

  it('renders the empty result state', () => {
    fixture.componentRef.setInput('response', {
      columns: [],
      rows: [],
      hasMore: false,
      returnedRowCount: 0,
    } satisfies ReportPreviewResponse);
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Aucune donnée');
  });

  it('keeps the previous rows visible while a new preview is loading', () => {
    fixture.componentRef.setInput('response', previousResponse());
    fixture.componentRef.setInput('stale', true);
    fixture.componentRef.setInput('loading', true);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('L’aperçu précédent reste affiché');
    expect(element.textContent).toContain('Ancienne ligne');
    expect(element.querySelectorAll('tbody tr').length).toBe(1);
  });

  it('shows an update error without discarding the previous rows and supports retry', () => {
    const component = fixture.componentInstance;
    let retried = false;
    component.retry.subscribe(() => (retried = true));
    fixture.componentRef.setInput('response', previousResponse());
    fixture.componentRef.setInput('stale', true);
    fixture.componentRef.setInput('error', 'Le serveur ne répond pas.');
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('dernier aperçu réussi');
    expect(element.textContent).toContain('Ancienne ligne');

    const retryButton = Array.from(element.querySelectorAll('button')).find((button) =>
      button.textContent?.includes('Réessayer'),
    );
    retryButton?.click();
    expect(retried).toBeTrue();
  });

  function previousResponse(): ReportPreviewResponse {
    return {
      columns: [{ key: 'field_1', fieldId: 1, displayName: 'Nom', dataType: 'TEXT' }],
      rows: [{ field_1: 'Ancienne ligne' }],
      hasMore: false,
      returnedRowCount: 1,
    };
  }
});
