import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ApiResponse } from '@core/authentication';
import { AuditLogDetail, AuditLogItem, PageResponse } from './audit-log.models';
import { AuditLogService } from './audit-log.service';

describe('AuditLogService', () => {
  let service: AuditLogService;
  let httpMock: HttpTestingController;

  const page: PageResponse<AuditLogItem> = {
    items: [
      {
        id: 1,
        occurredAt: '2026-08-14T08:30:00Z',
        actor: 'admin',
        action: 'UPDATE',
        entityType: 'PATIENT',
        entityId: 'P26AA001',
        targetLabel: 'P26AA001',
        details: 'Updated phone number',
      },
    ],
    pagination: {
      page: 2,
      pageSize: 50,
      totalItems: 51,
      totalPages: 2,
      hasNext: false,
      hasPrev: true,
    },
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AuditLogService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuditLogService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('loads a one-based audit-log page and unwraps the response envelope', () => {
    service.list({ page: 2, size: 50 }).subscribe(result => expect(result).toEqual(page));

    const request = httpMock.expectOne(
      request =>
        request.url === '/api/v1/audit-logs' &&
        request.params.get('page') === '2' &&
        request.params.get('size') === '50'
    );
    expect(request.request.method).toBe('GET');
    expect(request.request.params.keys()).toEqual(['page', 'size']);
    request.flush({
      success: true,
      statusCode: 200,
      message: 'Success',
      data: page,
      timestamp: '2026-08-14T08:31:00Z',
    } satisfies ApiResponse<PageResponse<AuditLogItem>>);
  });

  it('sends every applied filter and trims the keyword', () => {
    service
      .list({
        page: 1,
        size: 20,
        search: '  admin admission  ',
        actor: '  System Administrator  ',
        action: 'UPDATE',
        entityType: 'ADMISSION',
        from: '2026-08-01',
        to: '2026-08-23',
      })
      .subscribe(result => expect(result).toEqual(page));

    const request = httpMock.expectOne(
      request => request.url === '/api/v1/audit-logs' && request.params.get('page') === '1'
    );
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('size')).toBe('20');
    expect(request.request.params.get('search')).toBe('admin admission');
    expect(request.request.params.get('actor')).toBe('System Administrator');
    expect(request.request.params.get('action')).toBe('UPDATE');
    expect(request.request.params.get('entityType')).toBe('ADMISSION');
    expect(request.request.params.get('from')).toBe('2026-08-01');
    expect(request.request.params.get('to')).toBe('2026-08-23');
    request.flush({
      success: true,
      statusCode: 200,
      message: 'Success',
      data: page,
      timestamp: '2026-08-23T08:31:00Z',
    } satisfies ApiResponse<PageResponse<AuditLogItem>>);
  });

  it('loads structured details for one audit event', () => {
    const detail: AuditLogDetail = {
      event: {
        ...page.items[0],
        eventVersion: 2,
        legacy: false,
        summary: '1 field changed',
        changeCount: 1,
        affectedTargetCount: 0,
      },
      changes: [
        {
          fieldKey: 'phoneNo',
          valueType: 'TEXT',
          oldValue: '[REDACTED]',
          newValue: '[REDACTED]',
          oldDisplayValue: '[REDACTED]',
          newDisplayValue: '[REDACTED]',
          sensitivity: 'PERSONAL',
          displayOrder: 1,
        },
      ],
      affectedTargets: [],
    };

    service.getDetail(1).subscribe(result => expect(result).toEqual(detail));

    const request = httpMock.expectOne('/api/v1/audit-logs/1');
    expect(request.request.method).toBe('GET');
    request.flush({
      success: true,
      statusCode: 200,
      message: 'Success',
      data: detail,
      timestamp: '2026-08-23T08:31:00Z',
    } satisfies ApiResponse<AuditLogDetail>);
  });
});
