import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MtxDialog } from '@ng-matero/extensions/dialog';
import { HotToastService } from '@ngxpert/hot-toast';
import { Observable, of } from 'rxjs';
import { vi } from 'vitest';

import { AuthService } from '@core/authentication';
import { PatientList } from './patient-list';
import { PageResponse, PatientListItem } from './patient.models';
import { PatientQuery, PatientService } from './patient.service';

describe('PatientList filters', () => {
  let fixture: ComponentFixture<PatientList>;
  let component: PatientList;
  let list: (query: PatientQuery) => Observable<PageResponse<PatientListItem>>;

  const page: PageResponse<PatientListItem> = {
    items: [
      {
        id: 'P26AA001',
        name: 'Nguyen Van Test',
        gender: 1,
        birthDate: '1985-03-12',
        phoneNo: '0900000000',
        status: 1,
        hasHistory: false,
      },
    ],
    pagination: {
      page: 1,
      pageSize: 20,
      totalItems: 1,
      totalPages: 1,
      hasNext: false,
      hasPrev: false,
    },
  };

  beforeEach(async () => {
    list = vi.fn(() => of(page));

    await TestBed.configureTestingModule({
      imports: [PatientList],
      providers: [
        provideRouter([]),
        { provide: PatientService, useValue: { list } },
        { provide: AuthService, useValue: { hasPermission: () => false } },
        { provide: MatDialog, useValue: { open: vi.fn() } },
        { provide: MtxDialog, useValue: { confirm: vi.fn() } },
        { provide: HotToastService, useValue: { success: vi.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PatientList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  afterEach(() => fixture.destroy());

  it('updates the patient list automatically after a short filter pause', async () => {
    component.page = 3;

    component.filterForm.patchValue({ search: 'Nguyen', status: 1, gender: 1 });
    await waitForDebounce();

    expect(component.page).toBe(1);
    expect(list).toHaveBeenLastCalledWith({
      page: 1,
      size: 20,
      search: 'Nguyen',
      searchField: 'name',
      status: 1,
      gender: 1,
    });
  });

  it('keeps Reset as the only filter action and reloads cleared criteria', async () => {
    component.filterForm.setValue({
      searchField: 'phone',
      search: '0900',
      status: 2,
      gender: 1,
    });

    component.resetFilters();
    await waitForDebounce();

    const content = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(content).toContain('Reset');
    expect(content).not.toContain('Apply filters');
    expect(component.filterForm.getRawValue()).toEqual({
      searchField: 'name',
      search: '',
      status: null,
      gender: null,
    });
    expect(list).toHaveBeenLastCalledWith({
      page: 1,
      size: 20,
      search: '',
      searchField: 'name',
      status: null,
      gender: null,
    });
  });

  function waitForDebounce(): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, 350));
  }
});
