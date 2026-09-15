import { DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MtxGrid, MtxGridColumn } from '@ng-matero/extensions/grid';
import { Subscription, debounceTime } from 'rxjs';

import { AuditLogDetailDialog } from './audit-log-detail-dialog';
import {
  AUDIT_LOG_ACTIONS,
  AUDIT_LOG_ENTITY_TYPES,
  AuditLogAction,
  AuditLogEntityType,
  AuditLogFilters,
  AuditLogItem,
  auditActor,
  auditActorPrimary,
  auditActorSecondary,
  auditChangeSummary,
  auditEventLabel,
  auditTarget,
  auditTargetPrimary,
  auditTargetSecondary,
  readableAuditCode,
} from './audit-log.models';
import { AuditLogService } from './audit-log.service';

function auditDateRangeValidator(control: AbstractControl): ValidationErrors | null {
  const from = control.get('from')?.value as string | null | undefined;
  const to = control.get('to')?.value as string | null | undefined;
  return from && to && from > to ? { dateRange: true } : null;
}

@Component({
  selector: 'app-audit-log-page',
  templateUrl: './audit-log-page.html',
  styleUrl: './audit-log-page.scss',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatPaginatorModule,
    MatSelectModule,
    MtxGrid,
  ],
})
export class AuditLogPage implements OnInit {
  private readonly auditLogService = inject(AuditLogService);
  private readonly fb = inject(FormBuilder);
  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);
  private appliedFilters: AuditLogFilters = {};
  private loadSubscription?: Subscription;

  readonly actionOptions = AUDIT_LOG_ACTIONS;
  readonly entityTypeOptions = AUDIT_LOG_ENTITY_TYPES;
  readonly readableAuditCode = readableAuditCode;
  readonly eventLabel = auditEventLabel;
  readonly targetPrimary = auditTargetPrimary;
  readonly targetSecondary = auditTargetSecondary;
  readonly actorPrimary = auditActorPrimary;
  readonly actorSecondary = auditActorSecondary;
  readonly changeSummary = auditChangeSummary;
  readonly filterForm = this.fb.group(
    {
      search: ['', Validators.maxLength(200)],
      actor: ['', Validators.maxLength(100)],
      action: null as AuditLogAction | null,
      entityType: null as AuditLogEntityType | null,
      from: null as string | null,
      to: null as string | null,
    },
    { validators: auditDateRangeValidator }
  );

  readonly items = signal<AuditLogItem[]>([]);
  readonly total = signal(0);
  readonly loading = signal(true);
  readonly failed = signal(false);

  page = 1;
  size = 20;

  readonly columns: MtxGridColumn<AuditLogItem>[] = [
    {
      header: 'Time',
      field: 'occurredAt',
      type: 'date',
      typeParameter: { format: 'dd/MM/yyyy HH:mm:ss' },
      width: '165px',
    },
    {
      header: 'Event',
      field: 'event',
      formatter: row => auditEventLabel(row),
      width: '190px',
    },
    {
      header: 'Target',
      field: 'target',
      formatter: row => auditTarget(row),
      width: '280px',
    },
    {
      header: 'Performed by',
      field: 'performedBy',
      formatter: row => auditActor(row),
      width: '230px',
    },
    {
      header: 'Change summary',
      field: 'summary',
      formatter: row => auditChangeSummary(row),
      minWidth: 190,
    },
    {
      header: 'Details',
      field: 'detailsAction',
      type: 'button',
      width: '120px',
      pinned: 'right',
      buttons: [
        {
          type: 'text',
          text: 'View details',
          click: row => this.openDetails(row),
        },
      ],
    },
  ];

  ngOnInit(): void {
    this.load();
    this.filterForm.valueChanges
      .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.applyFilters());
  }

  load(): void {
    this.loading.set(true);
    this.failed.set(false);
    this.loadSubscription?.unsubscribe();
    this.loadSubscription = this.auditLogService
      .list({
        page: this.page,
        size: this.size,
        ...this.appliedFilters,
      })
      .subscribe({
        next: response => {
          this.items.set(response.items);
          this.total.set(response.pagination.totalItems);
          this.loading.set(false);
        },
        error: () => {
          this.items.set([]);
          this.total.set(0);
          this.failed.set(true);
          this.loading.set(false);
        },
      });
  }

  applyFilters(): void {
    if (this.filterForm.invalid) {
      this.loadSubscription?.unsubscribe();
      this.items.set([]);
      this.total.set(0);
      this.failed.set(false);
      this.loading.set(false);
      return;
    }

    const filters = this.filterForm.getRawValue();
    this.appliedFilters = {
      search: filters.search?.trim() || undefined,
      actor: filters.actor?.trim() || undefined,
      action: filters.action ?? undefined,
      entityType: filters.entityType ?? undefined,
      from: filters.from ?? undefined,
      to: filters.to ?? undefined,
    };
    this.page = 1;
    this.load();
  }

  clearFilters(): void {
    this.page = 1;
    this.filterForm.reset({
      search: '',
      actor: '',
      action: null,
      entityType: null,
      from: null,
      to: null,
    });
  }

  onPage(event: PageEvent): void {
    this.page = event.pageIndex + 1;
    this.size = event.pageSize;
    this.load();
  }

  openDetails(row: AuditLogItem): void {
    this.dialog.open(AuditLogDetailDialog, {
      width: '900px',
      maxWidth: '100vw',
      maxHeight: '100vh',
      autoFocus: false,
      ariaLabel: `Activity details for event ${row.id}`,
      data: { row },
      panelClass: 'audit-log-detail-dialog',
    });
  }
}
