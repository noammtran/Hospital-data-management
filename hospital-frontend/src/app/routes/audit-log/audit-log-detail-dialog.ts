import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import {
  AuditLogChange,
  AuditLogDetail,
  AuditLogItem,
  auditActorPrimary,
  auditActorSecondary,
  auditChangeSummary,
  auditChangeValue,
  auditEventLabel,
  auditFieldLabel,
  auditTargetPrimary,
  auditTargetSecondary,
  readableAuditCode,
} from './audit-log.models';
import { AuditLogService } from './audit-log.service';

export interface AuditLogDetailDialogData {
  row: AuditLogItem;
}

@Component({
  selector: 'app-audit-log-detail-dialog',
  templateUrl: './audit-log-detail-dialog.html',
  styleUrl: './audit-log-detail-dialog.scss',
  imports: [DatePipe, MatButtonModule, MatDialogModule, MatProgressSpinnerModule],
})
export class AuditLogDetailDialog implements OnInit {
  private readonly auditLogService = inject(AuditLogService);
  readonly data = inject<AuditLogDetailDialogData>(MAT_DIALOG_DATA);

  readonly detail = signal<AuditLogDetail | null>(null);
  readonly loading = signal(true);
  readonly failed = signal(false);

  readonly eventLabel = auditEventLabel;
  readonly actorPrimary = auditActorPrimary;
  readonly actorSecondary = auditActorSecondary;
  readonly targetPrimary = auditTargetPrimary;
  readonly targetSecondary = auditTargetSecondary;
  readonly changeSummary = auditChangeSummary;
  readonly fieldLabel = auditFieldLabel;
  readonly readableCode = readableAuditCode;

  ngOnInit(): void {
    this.auditLogService.getDetail(this.data.row.id).subscribe({
      next: detail => {
        this.detail.set(detail);
        this.loading.set(false);
      },
      error: () => {
        this.failed.set(true);
        this.loading.set(false);
      },
    });
  }

  oldValue(change: AuditLogChange): string {
    return auditChangeValue(change, 'old');
  }

  newValue(change: AuditLogChange): string {
    return auditChangeValue(change, 'new');
  }

  primaryTargetLabel(event: AuditLogItem): string {
    switch (event.entityType) {
      case 'PATIENT':
        return 'Patient';
      case 'ADMISSION':
        return 'Admission';
      case 'USER':
        return 'Staff account';
      default:
        return 'Target';
    }
  }

  secondaryTargetLabel(event: AuditLogItem): string {
    return event.entityType === 'ADMISSION' ? 'Patient' : 'Reference';
  }
}
