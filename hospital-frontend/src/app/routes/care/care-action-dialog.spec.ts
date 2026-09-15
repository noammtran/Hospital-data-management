import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { CareActionDialog, CareActionData } from './care-action-dialog';
import { CareService } from './care.service';
import { CareOptions, CareTask } from './care.models';
import { PatientService } from '../patient/patient.service';

const options: CareOptions = {
  currentUserId: 1,
  points: [
    { id: 10, code: 'LAB', name: 'Laboratory', kind: 'SERVICE', departmentId: 1, active: true },
  ],
  services: [{ id: 5, code: 'CBC', name: 'Blood count', pointId: 10, active: true }],
  staff: [],
  assignments: [],
  departments: [],
  doctors: [],
  rooms: [],
};
describe('CareActionDialog safeguards', () => {
  async function setup(data: Partial<CareActionData>) {
    const service = {
      taskAction: vi.fn(() => of({})),
      visitAction: vi.fn(() => of({})),
      register: vi.fn(() => of({})),
      medicines: vi.fn(() => of({ items: [] })),
    };
    const close = vi.fn();
    TestBed.configureTestingModule({
      imports: [CareActionDialog],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: { action: 'ORDER', options, ...data } },
        { provide: MatDialogRef, useValue: { close, disableClose: false } },
        { provide: CareService, useValue: service },
        { provide: PatientService, useValue: { list: vi.fn(() => of({ items: [] })) } },
      ],
    });
    const fixture = TestBed.createComponent(CareActionDialog);
    fixture.detectChanges();
    await fixture.whenStable();
    return { fixture, component: fixture.componentInstance, service, close };
  }
  it('does not send an empty service request', async () => {
    const { component, service } = await setup({});
    component.submit();
    expect(service.visitAction).not.toHaveBeenCalled();
    expect(component.form.invalid).toBe(true);
  });
  it('retains the same request key on a retry after a network error', async () => {
    const { component, service } = await setup({
      detail: { visit: { id: 'v' } } as CareActionData['detail'],
    });
    service.visitAction.mockReturnValue(throwError(() => ({ error: { message: 'Try again' } })));
    component.form.patchValue({ serviceIds: [5], reason: 'Fictional clinical indication' });
    component.submit();
    component.submit();
    expect(service.visitAction).toHaveBeenCalledTimes(2);
    const first = service.visitAction.mock.calls[0] as unknown as [
      string,
      string,
      { requestId: string; items: unknown[] },
    ];
    const second = service.visitAction.mock.calls[1] as unknown as typeof first;
    expect(first[2].requestId).toBe(second[2].requestId);
    expect(first[2].items).toEqual([{ serviceId: 5, reason: 'Fictional clinical indication' }]);
  });
  it('requires a correction reason after any published result', async () => {
    const task = {
      id: 1,
      results: [{ id: 4, revision: 1, body: 'Original', publishedAt: '2026-09-06T09:00:00' }],
    } as CareTask;
    const { component, service } = await setup({ action: 'RESULT', task });
    component.form.controls['body'].setValue('Corrected');
    component.submit();
    expect(service.taskAction).not.toHaveBeenCalled();
    component.form.controls['correctionReason'].setValue('Rechecked result');
    component.submit();
    expect(service.taskAction).toHaveBeenCalledTimes(1);
  });
  it('submits only the result identifier captured when review was opened', async () => {
    const task = {
      id: 1,
      results: [
        { id: 42, revision: 2, body: 'Reviewed result', publishedAt: '2026-09-06T09:00:00' },
      ],
    } as CareTask;
    const { component, service } = await setup({ action: 'ACKNOWLEDGE', task });
    component.submit();
    expect(service.taskAction).toHaveBeenCalledWith(1, 'acknowledge', { resultId: 42 });
  });
  it('keeps the form open and shows a failed save', async () => {
    const { component, service, close } = await setup({
      action: 'CANCEL_VISIT',
      detail: { visit: { id: 'v' } } as CareActionData['detail'],
    });
    component.form.controls['reason'].setValue('Incorrect registration');
    service.visitAction.mockReturnValue(
      throwError(() => ({ error: { message: 'Already received by doctor' } }))
    );
    component.submit();
    expect(close).not.toHaveBeenCalled();
    expect(component.error()).toBe('Already received by doctor');
    expect(component.saving()).toBe(false);
  });
});
