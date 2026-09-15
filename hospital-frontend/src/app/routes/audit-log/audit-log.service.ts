import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { ApiResponse } from '@core/authentication';
import { AuditLogDetail, AuditLogItem, AuditLogQuery, PageResponse } from './audit-log.models';

@Injectable({ providedIn: 'root' })
export class AuditLogService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/v1/audit-logs';

  list(query: AuditLogQuery): Observable<PageResponse<AuditLogItem>> {
    let params = new HttpParams().set('page', query.page).set('size', query.size);
    const search = query.search?.trim();

    if (search) {
      params = params.set('search', search);
    }
    const actor = query.actor?.trim();
    if (actor) {
      params = params.set('actor', actor);
    }
    if (query.action) {
      params = params.set('action', query.action);
    }
    if (query.entityType) {
      params = params.set('entityType', query.entityType);
    }
    if (query.from) {
      params = params.set('from', query.from);
    }
    if (query.to) {
      params = params.set('to', query.to);
    }

    return this.http
      .get<ApiResponse<PageResponse<AuditLogItem>>>(this.url, { params })
      .pipe(map(response => response.data));
  }

  getDetail(id: number): Observable<AuditLogDetail> {
    return this.http
      .get<ApiResponse<AuditLogDetail>>(`${this.url}/${id}`)
      .pipe(map(response => response.data));
  }
}
