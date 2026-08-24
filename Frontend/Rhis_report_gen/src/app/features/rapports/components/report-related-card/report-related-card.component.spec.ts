import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReportRelatedCardComponent } from './report-related-card.component';

describe('ReportRelatedCardComponent', () => {
  let fixture: ComponentFixture<ReportRelatedCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReportRelatedCardComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ReportRelatedCardComponent);
    fixture.componentRef.setInput('inputId', 'related-dataset-1-2');
    fixture.componentRef.setInput('title', 'Contrats');
    fixture.componentRef.setInput('icon', 'contract');
    fixture.detectChanges();
  });

  it('renders an accessible PrimeNG checkbox for the related table', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('article')).not.toBeNull();
    expect(element.querySelector('p-checkbox')).not.toBeNull();
    expect(element.querySelector('input[type="checkbox"]')).not.toBeNull();
    expect(element.querySelector('[aria-pressed]')).toBeNull();
    expect(element.textContent).toContain('Contrats');
    expect(element.querySelector('.report-related-card__description')).toBeNull();
  });
});
