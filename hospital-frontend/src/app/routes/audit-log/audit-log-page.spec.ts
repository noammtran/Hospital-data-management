import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { Observable, of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { AuditLogItem, AuditLogQuery, PageResponse } from './audit-log.models';
import { AuditLogPage } from './audit-log-page';
import { AuditLogService } from './audit-log.service';

describe('AuditLogPage', () => {
  let fixture: ComponentFixture<AuditLogPage>;
  let component: AuditLogPage;

  const response: PageResponse<AuditLogItem> = {
    items: [
      {
        id: 1,
        occurredAt: '2026-08-14T08:30:00Z',
        actor: 'admin',
        actorDisplayName: 'System Administrator',
        actorRole: 'ADMIN',
        action: 'UPDATE',
        entityType: 'ADMISSION',
        entityId: 'admission-1',
        targetLabel: 'Admission 14/08/2026 · admission-1',
        subjectType: 'PATIENT',
        subjectId: 'P26AA001',
        subjectLabel: 'Nguyen Van Test · P26AA001',
        details: 'Changed assigned room',
        eventVersion: 2,
        legacy: false,
        summary: '1 field changed',
        changeCount: 1,
        affectedTargetCount: 0,
      },
    ],
    pagination: {
      page: 1,
      pageSize: 20,
      totalItems: 41,
      totalPages: 3,
      hasNext: true,
      hasPrev: false,
    },
  };
  const auditLogService = {
    list: vi.fn((_query: AuditLogQuery): Observable<PageResponse<AuditLogItem>> => of(response)),
  };
  const dialog = { open: vi.fn() };

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [AuditLogPage],
      providers: [
        { provide: AuditLogService, useValue: auditLogService },
        { provide: MatDialog, useValue: dialog },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AuditLogPage);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  afterEach(() => fixture.destroy());

  it('renders the activity columns and the readable audit record', () => {
    const content = (fixture.nativeElement as HTMLElement).textContent ?? '';
    const actorInput = (fixture.nativeElement as HTMLElement).querySelector(
      'input[formcontrolname="actor"]'
    );

    expect(auditLogService.list).toHaveBeenCalledWith({ page: 1, size: 20 });
    expect(component.items()).toEqual(response.items);
    expect(component.total()).toBe(41);
    expect(component.columns.map(column => column.header)).toEqual([
      'Time',
      'Event',
      'Target',
      'Performed by',
      'Change summary',
      'Details',
    ]);
    expect(content).toContain('Activity history');
    expect(content).toContain('Search activity');
    expect(content).toContain('Performed by');
    expect(content).toContain('Action');
    expect(content).toContain('Target type');
    expect(content).toContain('From date');
    expect(content).toContain('To date');
    expect(content).toContain('Results update automatically');
    expect(content).not.toContain('Apply');
    expect(content).toContain('Clear');
    expect(content).toContain('Admission updated');
    expect(content).toContain('Admission 14/08/2026 · admission-1');
    expect(content).toContain('Nguyen Van Test · P26AA001');
    expect(content).toContain('System Administrator');
    expect(content).toContain('1 field changed');
    expect(content).toContain('View details');
    expect(actorInput?.getAttribute('placeholder')).toBe('Username or full name');
    expect(actorInput?.getAttribute('maxlength')).toBe('100');
  });

  it('opens the structured event detail dialog from a list row', () => {
    component.openDetails(response.items[0]);

    expect(dialog.open).toHaveBeenCalledWith(
      expect.any(Function),
      expect.objectContaining({
        width: '900px',
        data: { row: response.items[0] },
      })
    );
  });

  it('converts paginator indexes to one-based API pages and reloads', async () => {
    component.onPage({
      pageIndex: 1,
      previousPageIndex: 0,
      pageSize: 50,
      length: 41,
    } as PageEvent);
    await fixture.whenStable();

    expect(component.page).toBe(2);
    expect(component.size).toBe(50);
    expect(auditLogService.list).toHaveBeenLastCalledWith({ page: 2, size: 50 });
  });

  it('refreshes normalized filters automatically and preserves them while paging', async () => {
    component.page = 3;
    component.filterForm.setValue({
      search: '  admin admission  ',
      actor: '  System Administrator  ',
      action: 'UPDATE',
      entityType: 'ADMISSION',
      from: '2026-08-01',
      to: '2026-08-23',
    });

    await waitForDebounce();

    expect(component.page).toBe(1);
    expect(auditLogService.list).toHaveBeenLastCalledWith({
      page: 1,
      size: 20,
      search: 'admin admission',
      actor: 'System Administrator',
      action: 'UPDATE',
      entityType: 'ADMISSION',
      from: '2026-08-01',
      to: '2026-08-23',
    });

    component.onPage({ pageIndex: 1, pageSize: 10, length: 41 } as PageEvent);
    await fixture.whenStable();

    expect(auditLogService.list).toHaveBeenLastCalledWith({
      page: 2,
      size: 10,
      search: 'admin admission',
      actor: 'System Administrator',
      action: 'UPDATE',
      entityType: 'ADMISSION',
      from: '2026-08-01',
      to: '2026-08-23',
    });
  });

  it('blocks an inverted date range and shows a clear validation message', async () => {
    const callsBeforeChange = auditLogService.list.mock.calls.length;
    component.page = 3;
    component.filterForm.patchValue({ from: '2026-08-23', to: '2026-08-01' });

    await waitForDebounce();

    const content = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(component.page).toBe(3);
    expect(auditLogService.list).toHaveBeenCalledTimes(callsBeforeChange);
    expect(component.items()).toEqual([]);
    expect(component.total()).toBe(0);
    expect(content).toContain('From date must be on or before To date.');
  });

  it('blocks filters that exceed the backend length limits', async () => {
    const callsBeforeChange = auditLogService.list.mock.calls.length;
    component.filterForm.controls.search.markAsTouched();
    component.filterForm.controls.actor.markAsTouched();
    component.filterForm.patchValue({ search: 'x'.repeat(201), actor: 'a'.repeat(101) });

    await waitForDebounce();

    const content = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(auditLogService.list).toHaveBeenCalledTimes(callsBeforeChange);
    expect(content).toContain('Search must be 200 characters or fewer.');
    expect(content).toContain('Performed by must be 100 characters or fewer.');
  });

  it('clears stale rows and reports a failed filtered request', async () => {
    auditLogService.list.mockReturnValueOnce(throwError(() => new Error('Invalid filter request')));
    component.filterForm.patchValue({ actor: 'Unknown User' });

    await waitForDebounce();

    const content = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(component.items()).toEqual([]);
    expect(component.total()).toBe(0);
    expect(component.failed()).toBe(true);
    expect(content).toContain('Activity could not be loaded');
  });

  it('clears the draft and applied filters and returns to page one', async () => {
    component.filterForm.patchValue({ search: 'admin', action: 'CREATE' });
    await waitForDebounce();
    component.page = 2;

    component.clearFilters();
    await waitForDebounce();

    expect(component.page).toBe(1);
    expect(component.filterForm.getRawValue()).toEqual({
      search: '',
      actor: '',
      action: null,
      entityType: null,
      from: null,
      to: null,
    });
    expect(auditLogService.list).toHaveBeenLastCalledWith({ page: 1, size: 20 });
  });

  async function waitForDebounce(): Promise<void> {
    await new Promise(resolve => setTimeout(resolve, 350));
    await fixture.whenStable();
  }
});
