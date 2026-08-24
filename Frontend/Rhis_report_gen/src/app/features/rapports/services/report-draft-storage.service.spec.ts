import { TestBed } from '@angular/core/testing';

import { ReportDraftStorageService } from './report-draft-storage.service';

describe('ReportDraftStorageService', () => {
  let service: ReportDraftStorageService;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(ReportDraftStorageService);
  });

  it('keeps only the ordered lightweight definition', () => {
    const draft = {
      version: 1 as const,
      definition: {
        rootDatasetId: 1,
        selectedFieldIds: [12, 11],
        filters: [{ fieldId: 11, operator: 'EQUALS' as const, values: ['42'] }],
        sorts: [{ fieldId: 12, direction: 'DESC' as const }],
      },
      relatedDatasetIds: [3, 2],
    };

    service.save(draft);

    expect(service.load()).toEqual(draft);
    expect(JSON.stringify(service.load())).not.toContain('rows');
  });

  it('evicts corrupt storage instead of throwing', () => {
    sessionStorage.setItem('rhis.report.draft.v1', '{invalid');
    expect(service.load()).toBeNull();
    expect(sessionStorage.getItem('rhis.report.draft.v1')).toBeNull();
  });
});
