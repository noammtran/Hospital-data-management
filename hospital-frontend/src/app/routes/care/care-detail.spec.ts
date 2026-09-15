import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { EMPTY, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { AuthService } from '@core';
import { PatientService } from '../patient/patient.service';
import { CareDetail } from './care-detail';
import { CareDetail as Detail, CareOptions, CareTask } from './care.models';
import { CareService } from './care.service';

const options: CareOptions = {
  currentUserId: 1,
  points: [],
  services: [],
  staff: [],
  assignments: [],
  departments: [],
  doctors: [],
  rooms: [],
};
function task(changes: Partial<CareTask> = {}): CareTask {
  return {
    id: 10,
    visitId: 'visit-1',
    patientId: 'P26AA001',
    patientName: 'Fictional Patient',
    patientStatus: 0,
    pointId: 1,
    pointName: 'Clinic',
    kind: 'EXAM',
    serviceId: null,
    serviceName: null,
    status: 'QUEUED',
    arrival: 'EXPECTED',
    reason: null,
    createdBy: 2,
    orderedBy: 'Reception',
    createdAt: '2026-09-13T08:00:00',
    arrivedAt: null,
    startedAt: null,
    performedBy: null,
    resolution: null,
    resolvedAt: null,
    results: [],
    actions: ['ARRIVAL'],
    ...changes,
  };
}
function detail(tasks: CareTask[], changes: Partial<Detail> = {}): Detail {
  return {
    visit: {
      id: 'visit-1',
      patientId: 'P26AA001',
      patientName: 'Fictional Patient',
      patientStatus: 0,
      status: 'WAITING',
      progress: 'WAITING',
      pointId: 1,
      pointName: 'Clinic',
      locationPointId: null,
      locationName: null,
      ownerId: null,
      ownerName: null,
      priority: 0,
      createdAt: '2026-09-13T08:00:00',
      closedAt: null,
      orderCount: 0,
      publishedCount: 0,
      unreadCount: 0,
      critical: false,
      admissionId: null,
      wardPointId: null,
    },
    complaint: null,
    conclusion: null,
    plan: null,
    outcome: null,
    followupPlan: null,
    followupAt: null,
    followupOwner: null,
    tasks,
    notes: [],
    handoffs: [],
    diagnoses: [],
    actions: ['NOTE'],
    ...changes,
  };
}

describe('Care detail workflow buttons', () => {
  let fixture: ComponentFixture<CareDetail>;
  const care = { options: vi.fn(() => of(options)), taskAction: vi.fn(), visitAction: vi.fn() };
  const dialog = { open: vi.fn(() => ({ afterClosed: () => EMPTY })) };
  beforeEach(async () => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      imports: [CareDetail],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: EMPTY } },
        { provide: CareService, useValue: care },
        { provide: PatientService, useValue: {} },
        { provide: AuthService, useValue: { hasPermission: () => false } },
        { provide: MatDialog, useValue: dialog },
      ],
    });
    fixture = TestBed.createComponent(CareDetail);
    await fixture.whenStable();
  });
  async function render(value: Detail) {
    fixture.componentInstance.detail.set(value);
    await fixture.whenStable();
  }
  function labels(selector = '[aria-label="Room workflow actions"]'): string[] {
    return Array.from(fixture.nativeElement.querySelectorAll(`${selector} button`)).map(button =>
      (button as HTMLElement).textContent!.trim()
    );
  }
  function button(label: string): HTMLButtonElement {
    const found = Array.from(fixture.nativeElement.querySelectorAll('button')).find(
      element => (element as HTMLElement).textContent!.trim() === label
    );
    expect(found, label).toBeTruthy();
    return found as HTMLButtonElement;
  }

  it.each(['EXAM', 'ORDER'])(
    'shows only confirm arrived and absent before a %s starts',
    async kind => {
      await render(detail([task({ kind, serviceName: kind === 'ORDER' ? 'Blood count' : null })]));
      expect(labels()).toEqual(['Confirm arrived', 'Mark absent']);
      expect(labels('[aria-label="Visit actions"]')).toEqual([]);
    }
  );

  it('advances from expected to arrived, called and examining through the existing commands', async () => {
    const initial = detail([task()]);
    await render(initial);
    const arrived = detail([task({ arrival: 'ARRIVED', actions: ['ARRIVAL', 'START'] })]);
    arrived.visit.locationPointId = 1;
    care.taskAction.mockReturnValueOnce(of(arrived));
    button('Confirm arrived').click();
    await fixture.whenStable();
    expect(care.taskAction).toHaveBeenLastCalledWith(10, 'arrival', { state: 'ARRIVED' });
    expect(labels()).toEqual(['Call patient', 'Mark absent', 'Mark left', 'Start examination']);
    expect(labels('[aria-label="Visit actions"]')).toEqual(['Nursing observation']);

    const called = { ...arrived, tasks: [{ ...arrived.tasks[0], arrival: 'CALLED' }] };
    care.taskAction.mockReturnValueOnce(of(called));
    button('Call patient').click();
    await fixture.whenStable();
    expect(care.taskAction).toHaveBeenLastCalledWith(10, 'arrival', { state: 'CALLED' });
    expect(labels()).toEqual(['Mark absent', 'Mark left', 'Start examination']);

    const started = {
      ...called,
      visit: { ...called.visit, ownerId: 1, status: 'ACTIVE' },
      tasks: [{ ...called.tasks[0], status: 'IN_PROGRESS' }],
      actions: ['NOTE', 'ORDER', 'DIAGNOSIS', 'REVIEW'],
    };
    care.taskAction.mockReturnValueOnce(of(started));
    button('Start examination').click();
    await fixture.whenStable();
    expect(care.taskAction).toHaveBeenLastCalledWith(10, 'start', {});
    expect(labels()).toEqual(['Mark left']);
    expect(labels('[aria-label="Visit actions"]')).toContain('Request services');
    expect(dialog.open).not.toHaveBeenCalled();
  });

  it.each(['ABSENT', 'LEFT'])(
    'allows a %s patient to return without showing premature call/start buttons',
    async arrival => {
      await render(detail([task({ arrival, actions: ['ARRIVAL', 'START'] })]));
      expect(labels()).toEqual(['Confirm arrived']);
    }
  );

  it('resumes examination when the patient returns from services, then hides the repeated start', async () => {
    const value = detail([
      task({ arrival: 'ARRIVED', status: 'WAITING_RESULTS', actions: ['ARRIVAL', 'START'] }),
    ]);
    value.visit.ownerId = 1;
    value.visit.status = 'ACTIVE';
    await render(value);
    expect(labels()).toContain('Resume examination');
    await render({ ...value, tasks: [{ ...value.tasks[0], status: 'IN_PROGRESS' }] });
    expect(labels()).not.toContain('Resume examination');
  });

  it('keeps result entry after a performed service when the patient has left', async () => {
    await render(
      detail([
        task({
          kind: 'ORDER',
          status: 'IN_PROGRESS',
          arrival: 'LEFT',
          actions: ['ARRIVAL', 'RESULT'],
        }),
      ])
    );
    button('Result draft / correction').click();
    expect(dialog.open).toHaveBeenCalled();
    expect(labels()).not.toContain('Start service');
  });

  it.each(['PUBLISH', 'ACKNOWLEDGE'])(
    'keeps %s available after departure without arrival controls for a returned result',
    async action => {
      await render(
        detail([
          task({
            kind: 'ORDER',
            status: action === 'PUBLISH' ? 'AWAITING_VALIDATION' : 'RESULT_READY',
            arrival: 'LEFT',
            actions: ['ARRIVAL', action],
          }),
        ])
      );
      expect(labels()).toEqual([]);
      expect(
        button(action === 'PUBLISH' ? 'Validate & publish' : 'Confirm reviewed').disabled
      ).toBe(false);
    }
  );

  it('retains registration corrections and service cancellation before arrival as separate existing actions', async () => {
    await render(
      detail([task({ kind: 'ORDER', actions: ['CANCEL_ORDER', 'DECLINE_ORDER'] })], {
        actions: ['ROUTE', 'CANCEL_VISIT'],
      })
    );
    expect(labels()).toEqual([]);
    expect(labels('[aria-label="Visit coordination actions"]')).toEqual([
      'Change examination point',
      'Cancel registration',
    ]);
    expect(labels('[aria-label="Service request changes"]')).toEqual([
      'Cancel service',
      'Unable to perform',
    ]);
  });

  it('does not grant arrival or start actions from the patient state alone', async () => {
    const value = detail([task({ arrival: 'ARRIVED', actions: [] })], { actions: [] });
    await render(value);
    expect(labels()).toEqual([]);
    fixture.componentInstance.direct('start', value.tasks[0]);
    fixture.componentInstance.direct('arrival', value.tasks[0], 'CALLED');
    expect(care.taskAction).not.toHaveBeenCalled();
  });

  it('retains the previous stage and enables retry after a failed arrival request', async () => {
    await render(detail([task()]));
    care.taskAction.mockReturnValueOnce(throwError(() => ({ error: { message: 'Please retry' } })));
    button('Confirm arrived').click();
    await fixture.whenStable();
    expect(labels()).toEqual(['Confirm arrived', 'Mark absent']);
    expect(button('Confirm arrived').disabled).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('Please retry');
  });

  it.each(['COMPLETED', 'CANCELLED', 'DECLINED', 'CANCEL_REQUESTED'])(
    'does not show arrival controls for %s service work',
    async status => {
      await render(
        detail([task({ kind: 'ORDER', status, arrival: 'ARRIVED', actions: ['ARRIVAL', 'START'] })])
      );
      expect(labels()).toEqual([]);
    }
  );

  it('shows ward arrival only when the inpatient is away from the ward', async () => {
    const value = detail([], { actions: ['WARD_ARRIVAL', 'NOTE'] });
    value.visit.status = 'INPATIENT';
    value.visit.wardPointId = 4;
    value.visit.locationPointId = 2;
    await render(value);
    expect(labels('[aria-label="Visit actions"]')).toEqual(['Confirm arrival at ward']);
    await render({ ...value, visit: { ...value.visit, locationPointId: 4 } });
    expect(labels('[aria-label="Visit actions"]')).toEqual(['Nursing observation']);
  });
});
