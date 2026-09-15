import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { AuditLogDetail, AuditLogItem } from './audit-log.models';
import { AuditLogDetailDialog } from './audit-log-detail-dialog';
import { AuditLogService } from './audit-log.service';

describe('AuditLogDetailDialog', () => {
  let fixture: ComponentFixture<AuditLogDetailDialog> | undefined;
  let detailToReturn: AuditLogDetail;

  const event: AuditLogItem = {
    id: 42,
    occurredAt: '2026-08-23T08:30:00Z',
    actor: 'admin',
    actorDisplayName: 'System Administrator',
    actorRole: 'ADMIN',
    action: 'UPDATE',
    entityType: 'ADMISSION',
    entityId: 'admission-42',
    targetLabel: 'Admission 23/08/2026 · admission-42',
    subjectType: 'PATIENT',
    subjectId: 'P26AA001',
    subjectLabel: 'Nguyen Van Test · P26AA001',
    details: null,
    eventVersion: 2,
    legacy: false,
    summary: '1 field changed',
    changeCount: 1,
    affectedTargetCount: 0,
  };

  const roomChange = {
    fieldKey: 'roomId',
    valueType: 'REFERENCE' as const,
    oldValue: '10',
    newValue: '12',
    oldDisplayValue: 'Room A101',
    newDisplayValue: 'Room A103',
    sensitivity: 'GENERAL' as const,
    displayOrder: 1,
  };

  const auditLogService = {
    getDetail: vi.fn(() => of(detailToReturn)),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    detailToReturn = { event, changes: [roomChange], affectedTargets: [] };
    await TestBed.configureTestingModule({
      imports: [AuditLogDetailDialog],
      providers: [
        { provide: AuditLogService, useValue: auditLogService },
        { provide: MAT_DIALOG_DATA, useValue: { row: event } },
        { provide: MatDialogRef, useValue: { close: vi.fn() } },
      ],
    }).compileComponents();
  });

  afterEach(() => fixture?.destroy());

  async function render(): Promise<string> {
    fixture = TestBed.createComponent(AuditLogDetailDialog);
    await fixture.whenStable();
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  it('loads an event and renders an update as a Before and After comparison', async () => {
    const content = await render();

    expect(auditLogService.getDetail).toHaveBeenCalledWith(42);
    expect(content).toContain('Admission updated');
    expect(content).toContain('System Administrator');
    expect(content).toContain('Nguyen Van Test · P26AA001');
    expect(content).toContain('Before');
    expect(content).toContain('After');
    expect(content).toContain('Room A101');
    expect(content).toContain('Room A103');
  });

  it('renders created fields with new values only', async () => {
    detailToReturn = {
      event: { ...event, action: 'CREATE', summary: 'Admission created' },
      changes: [{ ...roomChange, oldValue: null, oldDisplayValue: null }],
      affectedTargets: [],
    };

    const content = await render();

    expect(content).toContain('New value');
    expect(content).toContain('Room A103');
    expect(content).not.toContain('Previous value');
  });

  it('renders deleted fields as their previous values', async () => {
    detailToReturn = {
      event: { ...event, action: 'DELETE', summary: 'Admission permanently deleted' },
      changes: [{ ...roomChange, newValue: null, newDisplayValue: null }],
      affectedTargets: [],
    };

    const content = await render();

    expect(content).toContain('Previous value');
    expect(content).toContain('Room A101');
    expect(content).not.toContain('New value');
  });

  it('renders every affected target for bulk deletion', async () => {
    detailToReturn = {
      event: {
        ...event,
        action: 'BULK_DELETE',
        entityType: 'PATIENT',
        summary: '2 patient records permanently deleted',
        changeCount: 0,
        affectedTargetCount: 2,
      },
      changes: [],
      affectedTargets: [
        {
          entityType: 'PATIENT',
          entityId: 'P26AA001',
          targetLabel: 'Nguyen Van Test · P26AA001',
          displayOrder: 1,
        },
        {
          entityType: 'PATIENT',
          entityId: 'P26AA002',
          targetLabel: 'Tran Van Test · P26AA002',
          displayOrder: 2,
        },
      ],
    };

    const content = await render();

    expect(content).toContain('Affected target');
    expect(content).toContain('Nguyen Van Test · P26AA001');
    expect(content).toContain('Tran Van Test · P26AA002');
  });

  it('explains that password values are never stored', async () => {
    detailToReturn = {
      event: {
        ...event,
        action: 'PASSWORD_RESET',
        entityType: 'USER',
        summary: 'Credentials reset',
        changeCount: 0,
      },
      changes: [],
      affectedTargets: [],
    };

    const content = await render();

    expect(content).toContain('Password values are never stored');
    expect(content).not.toContain('Before');
    expect(content).not.toContain('After');
  });

  it('shows an explicit compatibility message for a legacy event', async () => {
    detailToReturn = {
      event: {
        ...event,
        eventVersion: 1,
        legacy: true,
      },
      changes: [],
      affectedTargets: [],
    };

    const content = await render();

    expect(content).toContain('Detailed change data was not captured for this event.');
  });

  it('marks sample data so it cannot be mistaken for operational activity', async () => {
    detailToReturn = {
      event: { ...event, sampleData: true },
      changes: [roomChange],
      affectedTargets: [],
    };

    const content = await render();

    expect(content).toContain('Sample · Admission updated');
    expect(content).toContain('does not represent an operational action');
    expect(content).toContain('Sample data');
  });
});
