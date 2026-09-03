import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { ReportPreviewResponse } from '../../../../models/report-preview.model';
import { PreviewPanelComponent } from './preview-panel.component';

describe('PreviewPanelComponent', () => {
  let fixture: ComponentFixture<PreviewPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PreviewPanelComponent],
      providers: [provideNoopAnimations()],
    }).compileComponents();

    fixture = TestBed.createComponent(PreviewPanelComponent);
  });

  it('renders rows and cells in API column order with the returned count', () => {
    fixture.componentRef.setInput('status', 'À jour');
    fixture.componentRef.setInput('response', response());
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const headers = Array.from(element.querySelectorAll('thead th')).map((cell) =>
      cell.textContent?.trim(),
    );
    const cells = Array.from(element.querySelectorAll('tbody tr:first-child td')).map((cell) =>
      cell.textContent?.trim(),
    );

    expect(headers).toEqual(['Nom', 'Actif', 'Note', 'Commentaire']);
    expect(cells).toEqual(['Employé 1', 'Oui', '—', 'Texte']);
    expect(element.querySelectorAll('tbody tr').length).toBe(2);
    expect(element.textContent).toContain('2 lignes');
  });

  it('renders the empty success state and its zero count', () => {
    fixture.componentRef.setInput('status', 'À jour');
    fixture.componentRef.setInput('response', {
      columns: [],
      rows: [],
      hasMore: false,
      returnedRowCount: 0,
    } satisfies ReportPreviewResponse);
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('0 ligne');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Aucune donnée');
  });

  it('keeps previous rows visible while loading and after an error', () => {
    fixture.componentRef.setInput('response', response());
    fixture.componentRef.setInput('stale', true);
    fixture.componentRef.setInput('loading', true);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('dernier aperçu réussi');
    expect(element.querySelectorAll('tbody tr').length).toBe(2);

    fixture.componentRef.setInput('loading', false);
    fixture.componentRef.setInput('error', 'Le serveur ne répond pas.');
    fixture.detectChanges();

    expect(element.textContent).toContain('dernier aperçu réussi');
    expect(element.textContent).toContain('Le serveur ne répond pas.');
    expect(element.querySelectorAll('tbody tr').length).toBe(2);
  });

  it('renders initial loading and an error without a previous result', () => {
    fixture.componentRef.setInput('loading', true);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Chargement de l’aperçu');

    fixture.componentRef.setInput('loading', false);
    fixture.componentRef.setInput('error', 'Aperçu indisponible.');
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Aperçu indisponible');
    expect((fixture.nativeElement as HTMLElement).querySelector('table')).toBeNull();
  });

  it('shows stale and incomplete states without a dialog', () => {
    fixture.componentRef.setInput('response', response());
    fixture.componentRef.setInput('stale', true);
    fixture.componentRef.setInput('incompleteMessage', 'Sélectionnez au moins une colonne.');
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Sélectionnez au moins une colonne.');
    expect(element.textContent).toContain('dernière configuration validée');
    expect(element.querySelector('p-dialog')).toBeNull();
  });

  it('emits retry and keeps it visible when the body is collapsed', () => {
    let retried = false;
    fixture.componentInstance.retry.subscribe(() => (retried = true));
    fixture.componentRef.setInput('response', response());
    fixture.componentRef.setInput('error', 'Échec de mise à jour.');
    fixture.componentRef.setInput('collapsed', true);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const content = element.querySelector('#report-preview-body');
    expect(content?.classList).not.toContain('preview-content--expanded');
    expect(content?.hasAttribute('inert')).toBeTrue();
    expect(content?.getAttribute('aria-hidden')).toBe('true');
    expect(content?.querySelector(':scope > .preview-content-inner')).not.toBeNull();
    const retryButton = Array.from(element.querySelectorAll('button')).find((button) =>
      button.textContent?.includes('Réessayer'),
    );
    retryButton?.click();
    expect(retried).toBeTrue();
  });

  it('marks the preview content expanded without removing its rows', () => {
    fixture.componentRef.setInput('response', response());
    fixture.componentRef.setInput('collapsed', false);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('#report-preview-body')?.classList)
      .toContain('preview-content--expanded');
    expect(element.querySelector('#report-preview-body')?.hasAttribute('inert')).toBeFalse();
    expect(element.querySelectorAll('tbody tr').length).toBe(2);
    expect(element.textContent).toContain('Aperçu en temps réel');
  });

  function response(): ReportPreviewResponse {
    return {
      columns: [
        { key: 'name', fieldId: 1, displayName: 'Nom', dataType: 'TEXT' },
        { key: 'active', fieldId: 2, displayName: 'Actif', dataType: 'BOOLEAN' },
        { key: 'note', fieldId: 3, displayName: 'Note', dataType: 'TEXT' },
        { key: 'comment', fieldId: 4, displayName: 'Commentaire', dataType: 'TEXT' },
      ],
      rows: [
        { name: 'Employé 1', active: true, note: null, comment: 'Texte' },
        { name: 'Employé 2', active: false, comment: 'Suite' },
      ],
      hasMore: false,
      returnedRowCount: 2,
    };
  }
});
