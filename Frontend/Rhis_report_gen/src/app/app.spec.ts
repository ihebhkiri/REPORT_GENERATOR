import { TestBed } from '@angular/core/testing';
import { MessageService } from 'primeng/api';

import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [MessageService],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('renders one global toast and one router outlet', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('p-toast').length).toBe(1);
    expect(fixture.nativeElement.querySelectorAll('router-outlet').length).toBe(1);
  });
});
