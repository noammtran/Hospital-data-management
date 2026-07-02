import { DatePipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';

export interface StatisticsReportCriterion {
  label: string;
  value: string;
}

export interface StatisticsReportData {
  reportNo: string;
  generatedAt: Date;
  intro: string;
  criteria: StatisticsReportCriterion[];
  total: number;
  patients: { id: string; name: string; identityNo: string | null }[];
}

/** Read-only A4 report sheet shown in a popup over the statistics result. */
@Component({
  selector: 'app-statistics-report-dialog',
  templateUrl: './statistics-report-dialog.html',
  styleUrl: './statistics-report-dialog.scss',
  imports: [DatePipe, MatDialogModule, MatButtonModule],
})
export class StatisticsReportDialog {
  readonly data = inject<StatisticsReportData>(MAT_DIALOG_DATA);
}
