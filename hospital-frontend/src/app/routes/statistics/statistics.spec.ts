import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable, of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { StatisticsResult } from './statistics.models';
import { Statistics } from './statistics';
import { StatisticsFilter, StatisticsService } from './statistics.service';

describe('Statistics', () => {
  let fixture: ComponentFixture<Statistics>;
  let component: Statistics;

  const result: StatisticsResult = {
    total: 3,
    patients: [
      {
        id: 'P26AA001',
        name: 'Nguyen Van Test',
        identityNo: null,
        age: 44,
        gender: 1,
        status: 2,
        admissionCount: 2,
      },
    ],
    summary: {
      admissions: 4,
      diagnoses: 6,
      prescriptions: 8,
      patientStatuses: [
        { key: 'waiting', label: 'Waiting', count: 0 },
        { key: 'examining', label: 'Examining', count: 1 },
        { key: 'admitted', label: 'Admitted', count: 1 },
        { key: 'discharged', label: 'Discharged', count: 1 },
      ],
      genders: [
        { key: 'male', label: 'Male', count: 2 },
        { key: 'female', label: 'Female', count: 1 },
      ],
      ageGroups: [
        { key: '18-39', label: '18 to 39', count: 1 },
        { key: '40-59', label: '40 to 59', count: 2 },
      ],
    },
  };
  const service = {
    filter: vi.fn((_filter: StatisticsFilter): Observable<StatisticsResult> => of(result)),
    diagnosisOptions: vi.fn(() => of([{ icdCode: 'I10', label: 'I10 - Hypertension' }])),
    medicineOptions: vi.fn(() => of([{ medicineId: 23, label: 'Amlodipine 5mg' }])),
  };
  const dialog = { open: vi.fn() };

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [Statistics],
      providers: [
        provideNativeDateAdapter(),
        { provide: StatisticsService, useValue: service },
        { provide: MatDialog, useValue: dialog },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Statistics);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  afterEach(() => fixture.destroy());

  it('loads the default 30-day cohort and renders real dashboard summaries', () => {
    const content = (fixture.nativeElement as HTMLElement).textContent ?? '';
    const initialFilter = service.filter.mock.calls[0][0];

    expect(initialFilter.from).toBeTruthy();
    expect(initialFilter.to).toBeTruthy();
    expect(component.result()).toEqual(result);
    expect(component.activeCare()).toBe(2);
    expect(component.columns.map(column => column.header)).toEqual([
      'Patient ID',
      'Full name',
      'Age',
      'Gender',
      'Status',
      'Admissions',
      'Medical record',
    ]);
    expect(content).toContain('Clinical statistics');
    expect(content).toContain('Patients3');
    expect(content).toContain('Admissions4');
    expect(content).toContain('Diagnoses6');
    expect(content).toContain('Prescriptions8');
    expect(content).toContain('Patient status');
    expect(content).toContain('Demographic profile');
    expect(content).toContain('Results update automatically');
    expect(content).not.toContain('Apply filters');
  });

  it('reloads statistics automatically after a short filter pause', async () => {
    const callsBefore = service.filter.mock.calls.length;

    component.filterForm.patchValue({ icdCode: 'I10', medicineId: 23 });
    await waitForDebounce();

    expect(service.filter).toHaveBeenCalledTimes(callsBefore + 1);
    expect(service.filter).toHaveBeenLastCalledWith(
      expect.objectContaining({ icdCode: 'I10', medicineId: 23 })
    );
  });

  it('blocks an incomplete or reversed date range before calling the API', async () => {
    const callsBefore = service.filter.mock.calls.length;
    component.filterForm.patchValue({ from: new Date(2026, 7, 15), to: null });

    component.applyFilter();
    await fixture.whenStable();

    expect(service.filter).toHaveBeenCalledTimes(callsBefore);
    expect(component.dateRangeError()).toContain('both From and To');

    component.filterForm.patchValue({ from: new Date(2026, 7, 15), to: new Date(2026, 7, 14) });
    component.applyFilter();
    await fixture.whenStable();

    expect(service.filter).toHaveBeenCalledTimes(callsBefore);
    expect(component.dateRangeError()).toContain('on or after');
  });

  it('resets every filter and reloads the all-time cohort', async () => {
    component.filterForm.patchValue({ icdCode: 'I10', medicineId: 23 });

    component.resetFilters();
    await waitForDebounce();

    expect(component.filterForm.getRawValue()).toEqual({
      from: null,
      to: null,
      icdCode: null,
      medicineId: null,
    });
    expect(service.filter).toHaveBeenLastCalledWith({
      from: undefined,
      to: undefined,
      icdCode: undefined,
      medicineId: undefined,
    });
  });

  it('clears stale dashboard values and shows a contextual error', async () => {
    service.filter.mockReturnValueOnce(throwError(() => new Error('API unavailable')));

    component.applyFilter();
    await fixture.whenStable();

    const content = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(component.result()).toBeNull();
    expect(content).toContain('Unable to load statistics');
    expect(content).toContain('Check the filters and try again');
  });

  function waitForDebounce(): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, 350));
  }
});
